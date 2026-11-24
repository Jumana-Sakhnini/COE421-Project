/*
 * =====================================================================================
 *  COE421L Sentry Turret - TARGET BOARD (LilyPad)          COE421L_Project_Arduino
 * =====================================================================================
 *
 *  ROLE OF THIS CODE
 *  The LilyPad is the "hands and eyes" of the game; the Java program is the brain.
 *    1. It reads the sensors and sends a short text EVENT to Java over the XBee.
 *    2. It receives short text COMMANDS from Java and drives the LEDs and the buzzer.
 *  It decides nothing about the rules.
 *
 *  The GUN (IR emitter, trigger, vibration motor) is a SEPARATE Arduino. This board
 *  only has the IR RECEIVER, which reports a hit when it sees the gun's beam.
 *
 *  GAME (decided in Java)
 *    RED   (middle LED) -> shoot the IR target with the gun
 *    BLUE  (any LED)    -> press the push button under that LED
 *    GREEN (pattern)    -> watch the LEDs light one at a time, repeat with the joystick
 *                          LED 1 = LEFT, LED 2 = CENTER (push up or down), LED 3 = RIGHT
 *    Clap (microphone)  -> slows the game down
 *
 *  -----------------------------------------------------------------------------------
 *  WIRING (component pin -> LilyPad pin)            everything runs at 3.3 V
 *  -----------------------------------------------------------------------------------
 *  XBee  TX (DOUT) / RX (DIN) / VCC / GND  -> 10 / 11 / 3.3 V / GND   (SoftwareSerial)
 *  FTDI (3.3 V)                            -> 6-pin header (0 / 1)   upload + debugging
 *  RGB LED 1 / 2 / 3  "+"                  -> 6 / 7 / 8
 *  RGB LEDs R / G / B (joined)             -> 5 / 4 / 2   (red is really on 5, blue on 2)
 *  Push button 1/2/3  terminal 1           -> 6 / 7 / 8   (same wire as that LED's +)
 *  Push button 1/2/3  terminal 2           -> A4          (all three)
 *  Microphone  AO / + / G                  -> A3 / 3.3 V / GND
 *  IR receiver (target 2)  S / + / -       -> A0 / 3.3 V / GND
 *  Passive buzzer  +  -> 3                 -> for MAXIMUM volume: buzzer "-" -> 9
 *                                             (or "-" -> GND and set BUZZER_PUSH_PULL false)
 *  Joystick  VRx / VRy / SW / VCC / GND    -> A1 / A2 / 12 / 3.3 V / GND
 *
 *  -----------------------------------------------------------------------------------
 *  MESSAGES  (text lines ending with a newline, 9600 baud)
 *  -----------------------------------------------------------------------------------
 *  LilyPad -> Java (events)
 *    READY                       board started (also the answer to PING)
 *    SHOT,2                      the IR receiver detected one shot from the gun
 *    BUTTON_PRESS,n              push button n (1-3) pressed
 *    JOYSTICK,LEFT|CENTER|RIGHT  joystick pushed (CENTER = pushed up or down)
 *    CLAP                        clap detected (Java slows the game down)
 *    SCAN_DONE                   the colour scan has finished
 *
 *  Java -> LilyPad (commands)
 *    LIGHT,id,COLOR              LED id (1-3) to RED / BLUE / GREEN / WHITE / OFF
 *    FLICKER,id,COLOR            fake target: a DIM colour on LED id for FLICKER_MS
 *    SCAN,step,dur               colour scan (see below), step ms per LED, for dur ms
 *    OFF                         all LEDs off, scan / flicker stopped
 *    SOUND,TYPE,0                START SUCCESS COMBO FAILURE TICK SLOW LEVEL_UP DEAD SESSION_END
 *    PING                        reply READY
 *
 *  The same commands can be typed in the Serial Monitor (FTDI) to test the outputs.
 *
 *  Board : LilyPad Arduino, ATmega328P (3.3 V, 8 MHz)      Libraries : SoftwareSerial
 * =====================================================================================
 */

#include <SoftwareSerial.h>

// =====================================================================================
// PINS
// =====================================================================================
const uint8_t PIN_LED_R = 5;                 // red legs of LED1+2+3 (old table said 2, tested: 5)
const uint8_t PIN_LED_G = 4;                 // green legs of all three
const uint8_t PIN_LED_B = 2;                 // blue legs (old table said 5, tested: 2)
const uint8_t PIN_LED_PLUS[3] = {6, 7, 8};   // "+" of LED1/2/3 = terminal 1 of button 1/2/3
const uint8_t PIN_BTN     = A4;              // terminal 2 of all three buttons
const uint8_t PIN_MIC     = A3;              // microphone analog output
const uint8_t PIN_IR_RX   = A0;              // IR receiver output (LOW while it sees 38 kHz IR)
const uint8_t PIN_BUZZER  = 3;               // buzzer "+"   (ATmega328 port D, bit 3)
const uint8_t PIN_BUZZER2 = 9;               // buzzer "-"   (ATmega328 port B, bit 1)
const uint8_t PIN_JOY_X   = A1;              // joystick VRx
const uint8_t PIN_JOY_Y   = A2;              // joystick VRy
const uint8_t PIN_XBEE_RX = 10;              // LilyPad receives here  <- XBee TX (DOUT)
const uint8_t PIN_XBEE_TX = 11;              // LilyPad sends here     -> XBee RX (DIN)

SoftwareSerial xbee(PIN_XBEE_RX, PIN_XBEE_TX);

// =====================================================================================
// DATA TYPES (at the top so the Arduino IDE's automatic prototypes know them)
// =====================================================================================
struct Note { uint16_t freq; uint16_t ms; };        // one buzzer note, freq 0 = silence

struct LineBuffer { char buf[32]; uint8_t len; };   // collects one incoming command line

// =====================================================================================
// SETTINGS / CALIBRATION
// =====================================================================================
const bool     DEBUG_TO_USB      = true;   // also print every event on the FTDI Serial Monitor
const uint8_t  IR_TARGET_ID      = 2;      // the IR receiver sits under the middle LED

// --- buzzer ---
const bool     BUZZER_PUSH_PULL  = true;   // true: buzzer between pins 3 and 9, driven in
                                           // opposite phase = twice the voltage swing
                                           // (about 4x the power, the loudest possible
                                           // without extra parts). false: "-" on GND.

// --- microphone (clap = slow the game down) ---
const int      CLAP_THRESHOLD    = 45;     // lower = more sensitive
const uint16_t CLAP_WINDOW_MS    = 30;     // the signal swing is measured over this window
const uint16_t CLAP_PEAK_MS      = 120;    // keep listening this long to catch the clap's peak
const uint16_t CLAP_IGNORE_MS    = 250;    // ignore the echo of the same clap
const uint16_t MIC_QUIET_MS      = 250;    // ignore the mic this long after the buzzer stops

// --- IR receiver: only real shots count, stray IR in the air is ignored ---
// A shot from the gun is ONE steady beam of about 250 ms. Stray IR (lamps, sunlight,
// phones, TV remotes, reflections, a far-away gun) gives short, scattered or very long
// signals. A hit needs ALL of these:
//   - in each IR_WINDOW_MS window at least IR_MIN_LOW_PERCENT of the readings show IR
//     (scattered blips never get there),
//   - those dense windows follow each other for at least IR_MIN_BEAM_MS (short bursts
//     and remote-control codes are too short),
//   - the beam then STOPS within IR_MAX_BEAM_MS (constant interference never stops),
//   - the receiver was not already busy (IR_NOISY_PERCENT or more) for IR_QUIET_BEFORE_MS
//     before the beam started. Single random blips do not count as busy, so a light
//     background of stray IR does not block real shots.
// The hit is reported when the beam ends (about 0.3 s after the trigger).
const uint16_t IR_WINDOW_MS        = 20;
const uint8_t  IR_MIN_LOW_PERCENT  = 40;   // false hits -> raise (50, 60); real shots missed -> lower (30)
const uint16_t IR_MIN_BEAM_MS      = 80;   // shortest beam that counts as a shot
const uint16_t IR_MAX_BEAM_MS      = 1000; // longer than this = interference, not a shot
const uint8_t  IR_MAX_GAP_WINDOWS  = 1;    // one weak window inside a beam is tolerated
const uint16_t IR_QUIET_BEFORE_MS  = 100;  // receiver must not be busy this long before a beam
const uint8_t  IR_NOISY_PERCENT    = 15;   // a window with this much IR counts as "busy"
const bool     IR_DEBUG            = false; // true = print every beam seen and why it was ignored

// --- buttons, joystick ---
const uint8_t  DEBOUNCE_SCANS    = 4;      // push buttons: ~6 ms per scan -> ~24 ms debounce
// same values as the working TEST 8 joystick sketch
const int      JOY_PUSH          = 300;    // distance from centre (0-512) that counts as a push
const int      JOY_RELEASE       = 120;    // back within this = stick returned to the centre
const uint8_t  JOY_CONFIRM_READS = 2;      // a push must be seen in 2 readings in a row (40 ms)
const bool     JOY_SWAP_XY       = false;  // set true if up/down and left/right are mixed up
const bool     JOY_INVERT_X      = false;  // set true if LEFT and RIGHT are reversed
const uint16_t JOY_READ_EVERY_MS = 20;
const bool     JOY_DEBUG         = false; // true: Serial Monitor shows raw X / Y / button every 0.2 s

// --- LEDs ---
const uint16_t FLICKER_MS        = 150;    // length of a fake (dim) flicker (Java uses 150 too)

// =====================================================================================
// RGB LEDs (multiplexed)
// LilyPad tri-colour LEDs are common anode: "+" HIGH selects an LED, a colour pin LOW
// lights that colour. Only one LED is lit at a time, 2 ms each, which looks continuous.
// =====================================================================================
const uint8_t OFF = 0, RED = 1, GREEN = 2, BLUE = 4, WHITE = 7;

uint8_t ledColor[3] = {OFF, OFF, OFF};        // colour of each LED
bool    ledDim[3]   = {false, false, false};  // dim = lit in only 1 of 4 refresh cycles
uint8_t muxIdx = 0;                     // LED being shown right now
uint8_t muxCycle = 0;                   // counts full refresh cycles (for dimming)
unsigned long muxLast = 0;

// converts a colour word from a command (RED, BLUE, GREEN, WHITE, OFF) into a colour
uint8_t colourFromWord(const char *w) {
  if (!w) return OFF;
  switch (toupper(w[0])) {
    case 'R': return RED;
    case 'G': return GREEN;
    case 'B': return BLUE;
    case 'W': return WHITE;
    default:  return OFF;               // "OFF" or anything unknown
  }
}

void allOff() {
  for (uint8_t i = 0; i < 3; i++) { ledColor[i] = OFF; ledDim[i] = false; }
}

// =====================================================================================
// PUSH BUTTONS (share the LED "+" wires)
// After every full LED refresh cycle the colours are switched off, A4 gets its pull-up
// and each "+" pin is pulled LOW in turn. If A4 reads LOW, that button is pressed.
// Unused "+" pins are left floating (INPUT) so two pressed buttons can never short.
// =====================================================================================
int8_t  btnRaw = -1, btnStable = -1;    // -1 = none, 0..2 = button index
uint8_t btnCount = 0;

void plusOff(uint8_t i) {               // "+" pin floating = LED off
  digitalWrite(PIN_LED_PLUS[i], LOW);
  pinMode(PIN_LED_PLUS[i], INPUT);
}

int8_t scanButtons() {
  digitalWrite(PIN_LED_R, HIGH);        // all colours off during the scan
  digitalWrite(PIN_LED_G, HIGH);
  digitalWrite(PIN_LED_B, HIGH);
  for (uint8_t i = 0; i < 3; i++) plusOff(i);
  pinMode(PIN_BTN, INPUT_PULLUP);

  int8_t found = -1;
  for (uint8_t j = 0; j < 3 && found < 0; j++) {
    pinMode(PIN_LED_PLUS[j], OUTPUT);   // output latch is LOW -> this "+" is driven LOW
    delayMicroseconds(20);
    if (digitalRead(PIN_BTN) == LOW) found = j;
    pinMode(PIN_LED_PLUS[j], INPUT);
  }
  pinMode(PIN_BTN, INPUT);              // pull-up off so no LED glows faintly
  return found;
}

void sendEvent(const char *msg);                    // defined further down
void sendEventNum(const char *name, int value);     // defined further down

// debounces the scan result; sends "BUTTON_PRESS,n" once per new press
void debounceButtons(int8_t raw) {
  if (raw != btnRaw) { btnRaw = raw; btnCount = 0; return; }
  if (btnCount < DEBOUNCE_SCANS) {
    btnCount++;
    if (btnCount == DEBOUNCE_SCANS && raw != btnStable) {
      if (raw >= 0) sendEventNum("BUTTON_PRESS", raw + 1);
      btnStable = raw;
    }
  }
}

// shows LED1, LED2, LED3 in turn (2 ms each) and scans the buttons once per cycle
void updateMux() {
  if (micros() - muxLast < 2000) return;
  muxLast = micros();

  plusOff(muxIdx);                      // switch the previous LED off
  muxIdx = (muxIdx + 1) % 3;
  if (muxIdx == 0) {                    // one full cycle done
    muxCycle++;
    debounceButtons(scanButtons());
  }

  uint8_t c = ledColor[muxIdx];
  if (ledDim[muxIdx] && (muxCycle & 3) != 0) c = OFF;   // dim = lit 1 cycle in 4

  digitalWrite(PIN_LED_R, (c & RED)   ? LOW : HIGH);
  digitalWrite(PIN_LED_G, (c & GREEN) ? LOW : HIGH);
  digitalWrite(PIN_LED_B, (c & BLUE)  ? LOW : HIGH);
  pinMode(PIN_LED_PLUS[muxIdx], OUTPUT);
  digitalWrite(PIN_LED_PLUS[muxIdx], HIGH);
}

// =====================================================================================
// COLOUR SCAN ("SCAN" command)
// One LED is lit at a time, moving TOWARD THE LEFT (LED 3 -> 2 -> 1 -> 3 -> 2 -> 1 ...)
// and the colour changes every step: RED -> GREEN -> BLUE -> RED ...
// The Java screen draws exactly the same sequence.
// =====================================================================================
const uint8_t SCAN_COLOURS[3] = {RED, GREEN, BLUE};
bool sweeping = false;
uint16_t sweepStepMs = 300;
unsigned long sweepEnd = 0, sweepNextStep = 0;
uint16_t sweepStep = 0;                 // step counter: position and colour come from it

void startSweep(uint16_t stepMs, uint16_t durationMs) {
  allOff();
  sweeping = true;
  sweepStepMs = stepMs < 20 ? 20 : stepMs;
  sweepEnd = millis() + durationMs;
  sweepNextStep = millis();
  sweepStep = 0;
}

void updateSweep() {
  if (!sweeping) return;
  unsigned long now = millis();
  if ((long)(now - sweepEnd) >= 0) {    // scan time over
    sweeping = false;
    allOff();
    sendEvent("SCAN_DONE");
    return;
  }
  if ((long)(now - sweepNextStep) >= 0) {
    allOff();
    uint8_t led = 2 - (sweepStep % 3);           // 2, 1, 0, 2, 1, 0 ... (right to left)
    ledColor[led] = SCAN_COLOURS[sweepStep % 3]; // red, green, blue, red ...
    sweepStep++;
    sweepNextStep = now + sweepStepMs;
  }
}

// =====================================================================================
// FAKE FLICKER ("FLICKER" command) - Level 3 trick: a dim colour that must be ignored
// =====================================================================================
int8_t flickerLed = -1;                 // LED showing a fake flicker, -1 = none
unsigned long flickerEnd = 0;

void startFlicker(int8_t led, uint8_t colour) {
  sweeping = false;
  allOff();
  flickerLed = led;
  ledColor[led] = colour;
  ledDim[led] = true;                   // dim, so an attentive player can tell it is fake
  flickerEnd = millis() + FLICKER_MS;
}

void updateFlicker() {
  if (flickerLed < 0 || (long)(millis() - flickerEnd) < 0) return;
  ledColor[flickerLed] = OFF;
  ledDim[flickerLed] = false;
  flickerLed = -1;
}

// =====================================================================================
// BUZZER DRIVER - as loud as the LilyPad can make it
// A square wave is made by Timer1's interrupt, which flips pin 3 and (push-pull) pin 9
// at the same moment in OPPOSITE directions. With the buzzer between pins 3 and 9 it
// sees +3.3 V then -3.3 V: twice the swing of a buzzer to GND, about 4x the power.
// tone() cannot do this (it drives one pin only).
// =====================================================================================
volatile bool buzzing = false;

ISR(TIMER1_COMPA_vect) {                // runs twice per period of the note
  PIND = _BV(3);                        // writing 1 to PINx flips the pin: pin 3 (PD3)
  if (BUZZER_PUSH_PULL) PINB = _BV(1);  // pin 9 (PB1), always opposite to pin 3
}

void buzzerOff() {
  TIMSK1 &= ~_BV(OCIE1A);
  TCCR1B = 0;
  buzzing = false;
  digitalWrite(PIN_BUZZER, LOW);
  digitalWrite(PIN_BUZZER2, LOW);
}

void buzzerTone(uint16_t freq) {
  if (freq == 0) { buzzerOff(); return; }
  uint32_t top = F_CPU / (2UL * freq) - 1;          // prescaler 1
  uint8_t clock = _BV(CS10);
  if (top > 65535UL) { top = F_CPU / (16UL * freq) - 1; clock = _BV(CS11); }   // prescaler 8
  noInterrupts();
  if (!buzzing) {                                   // start with the two pins opposite
    digitalWrite(PIN_BUZZER, HIGH);
    digitalWrite(PIN_BUZZER2, LOW);
  }
  TCCR1A = 0;
  TCCR1B = _BV(WGM12) | clock;                      // CTC mode: interrupt every 'top' counts
  OCR1A  = top;
  TCNT1  = 0;
  TIMSK1 |= _BV(OCIE1A);
  buzzing = true;
  interrupts();
}

// =====================================================================================
// SOUNDS - non-blocking melody player
// RIGHT is high and bright (3.5-4.7 kHz, near the buzzer's loudest frequency).
// WRONG is low, falling and growling - the "sad trombone + buzzer" of game shows.
// =====================================================================================
const Note SND_START[]    = {{2093,110},{2637,110},{3136,110},{4186,380}};             // rising call
const Note SND_SUCCESS[]  = {{3520,60},{0,15},{4186,60},{0,15},{4699,220}};            // RIGHT: bright "ting-ting-TING"
const Note SND_COMBO[]    = {{2637,45},{3136,45},{3520,45},{4186,45},{4699,45},{5274,240}};  // fast sparkle up
const Note SND_FAILURE[]  = {{311,170},{0,30},{294,170},{0,30},{277,170},{0,30},     // WRONG: "wah-wah-wah-
                             {262,28},{233,28},{262,28},{233,28},{262,28},{233,28},  //  wahhhh" with a growl
                             {262,28},{233,28},{262,28},{233,28},{262,28},{233,28}};
const Note SND_TICK[]     = {{4186,35}};                                               // joystick step ok
const Note SND_SLOW[]     = {{1568,110},{1175,110},{880,240}};                         // clap: slowing down
const Note SND_LEVEL[]    = {{2637,90},{3136,90},{3951,90},{0,50},{3951,90},{4699,320}};
const Note SND_DEAD[]     = {{784,250},{698,250},{622,250},{523,280},{0,40},
                             {262,40},{233,40},{262,40},{233,40},{262,40},{233,40}}; // officer down
const Note SND_SESSION[]  = {{2093,120},{2637,120},{3136,120},{4186,260},{0,80},{3136,120},{4186,480}}; // fanfare

#define SOUND(arr) arr, (uint8_t)(sizeof(arr) / sizeof(arr[0]))

const Note *song = NULL;                // melody being played
uint8_t songLen = 0, songIdx = 0;
bool soundPlaying = false;
unsigned long noteEnd = 0;
unsigned long micQuietUntil = 0;        // mic ignored until this time

void startNote() {
  if (songIdx >= songLen) {             // melody finished
    buzzerOff();
    soundPlaying = false;
    micQuietUntil = millis() + MIC_QUIET_MS;
    return;
  }
  buzzerTone(song[songIdx].freq);
  noteEnd = millis() + song[songIdx].ms;
  soundPlaying = true;
}

void playSound(const Note *s, uint8_t len) {   // a new sound replaces the current one
  song = s; songLen = len; songIdx = 0;
  startNote();
}

void updateSound() {
  if (soundPlaying && (long)(millis() - noteEnd) >= 0) { songIdx++; startNote(); }
}

// plays the sound named in a "SOUND,TYPE,0" command
void playSoundByName(const char *name) {
  if (!name) return;
  if      (!strcmp(name, "START"))       playSound(SOUND(SND_START));
  else if (!strcmp(name, "SUCCESS"))     playSound(SOUND(SND_SUCCESS));
  else if (!strcmp(name, "COMBO"))       playSound(SOUND(SND_COMBO));
  else if (!strcmp(name, "FAILURE"))     playSound(SOUND(SND_FAILURE));
  else if (!strcmp(name, "TICK"))        playSound(SOUND(SND_TICK));
  else if (!strcmp(name, "SLOW"))        playSound(SOUND(SND_SLOW));
  else if (!strcmp(name, "LEVEL_UP"))    playSound(SOUND(SND_LEVEL));
  else if (!strcmp(name, "DEAD"))        playSound(SOUND(SND_DEAD));
  else if (!strcmp(name, "SESSION_END")) playSound(SOUND(SND_SESSION));
}

// =====================================================================================
// IR RECEIVER: one steady beam from the gun -> "SHOT,2"   (rules: see the settings)
// =====================================================================================
unsigned long irWinStart = 0;
uint16_t irSamples = 0, irLows = 0;
bool     irInBeam = false;
unsigned long irBeamStart = 0;
uint8_t  irGapWindows = 0;
unsigned long irNoisyLast = 0;          // end time of the last "busy" window
unsigned long irNoisyPrev = 0;          // ...and of the busy window before that one
bool     irBeamValidStart = false;      // the receiver was quiet before this beam

void irNoteNoisy(unsigned long windowEnd) {
  irNoisyPrev = irNoisyLast;
  irNoisyLast = windowEnd;
}

void updateIrReceiver() {
  unsigned long now = millis();
  irSamples++;
  if (digitalRead(PIN_IR_RX) == LOW) irLows++;
  if (now - irWinStart < IR_WINDOW_MS) return;

  // ---- judge the finished window ----
  bool dense = irSamples >= 5 && (uint32_t)irLows * 100 >= (uint32_t)irSamples * IR_MIN_LOW_PERCENT;
  bool anyIr = (uint32_t)irLows * 100 >= (uint32_t)irSamples * IR_NOISY_PERCENT;   // "busy"
  unsigned long winStart = irWinStart;
  irWinStart = now;
  irSamples = 0;
  irLows = 0;

  if (!irInBeam) {
    if (dense) {                                     // a beam may be starting
      irInBeam = true;
      irBeamStart = winStart;
      irGapWindows = 0;
      // A real beam often starts in the middle of the window before this one, so that
      // window may hold a little IR: it is skipped, and the quiet time is checked before it.
      unsigned long quietEnd = winStart, lastNoise = irNoisyLast;
      if (irNoisyLast == winStart) { quietEnd = winStart - IR_WINDOW_MS; lastNoise = irNoisyPrev; }
      irBeamValidStart = (quietEnd - lastNoise) >= IR_QUIET_BEFORE_MS || lastNoise == 0;
    }
    if (anyIr) irNoteNoisy(now);
    return;
  }

  if (dense) { irGapWindows = 0; return; }           // beam continues
  if (++irGapWindows <= IR_MAX_GAP_WINDOWS) return;            // short dip: tolerated

  // ---- the beam has ended: was it a real shot? ----
  unsigned long length = (now - irBeamStart) - (unsigned long)irGapWindows * IR_WINDOW_MS;
  irInBeam = false;
  irNoteNoisy(now);                                  // the beam itself was IR
  bool ok = irBeamValidStart && length >= IR_MIN_BEAM_MS && length <= IR_MAX_BEAM_MS;
  if (ok) sendEventNum("SHOT", IR_TARGET_ID);
  if (IR_DEBUG) {
    Serial.print(ok ? F("  IR shot, beam ") : F("  IR ignored, beam "));
    Serial.print(length);
    Serial.println(irBeamValidStart ? F(" ms") : F(" ms (noisy before)"));
  }
}

// =====================================================================================
// JOYSTICK: LEFT / RIGHT, CENTER = pushed up or down (the joystick's push button is not used)
// One event per real push: the push must be seen JOY_CONFIRM_READS readings in a row,
// nothing more is sent until the stick is back near the centre, the centre follows slow
// drift (only while it rests in the middle).
// =====================================================================================

const int8_t DIR_LEFT = 0, DIR_RIGHT = 1, DIR_CENTER = 2;
int joyCentreX = 512, joyCentreY = 512;
bool joyAtCentre = true;                // true = ready for a new push
unsigned long joyLastRead = 0;
int8_t joyCandidate = -1;               // direction seen in the last readings (-1 = none)
uint8_t joyCandidateCount = 0;
unsigned long joyDebugAt = 0;

void sendDirection(int8_t dir) {
  if (dir == DIR_RIGHT)     sendEvent("JOYSTICK,RIGHT");
  else if (dir == DIR_LEFT) sendEvent("JOYSTICK,LEFT");
  else                      sendEvent("JOYSTICK,CENTER");   // up or down = middle LED
}

void calibrateJoystick() {              // stick must be untouched at power-up
  long sx = 0, sy = 0;
  for (uint8_t i = 0; i < 16; i++) {
    sx += analogRead(PIN_JOY_X);
    sy += analogRead(PIN_JOY_Y);
    delay(5);
  }
  joyCentreX = sx / 16;
  joyCentreY = sy / 16;
  // an untouched stick reads about 512; far from that = VRx/VRy wires are mixed up
  if (joyCentreX < 150 || joyCentreX > 870 || joyCentreY < 150 || joyCentreY > 870) {
    Serial.print(F("WARNING: joystick not centred at start-up (X="));
    Serial.print(joyCentreX); Serial.print(F(" Y=")); Serial.print(joyCentreY);
    Serial.println(F(") - check VRx->A1, VRy->A2"));
  }
}

void updateJoystick() {
  unsigned long now = millis();

  if (millis() - joyLastRead < JOY_READ_EVERY_MS) return;
  joyLastRead = millis();

  int rawX = analogRead(PIN_JOY_X);
  int rawY = analogRead(PIN_JOY_Y);
  int dx = rawX - joyCentreX;
  int dy = rawY - joyCentreY;
  if (JOY_SWAP_XY) { int t = dx; dx = dy; dy = t; }
  if (JOY_INVERT_X) dx = -dx;

  if (JOY_DEBUG && now - joyDebugAt >= 200) {
    joyDebugAt = now;
    Serial.print(F("JOY X=")); Serial.print(rawX);
    Serial.print(F(" Y=")); Serial.print(rawY);
    Serial.print(F("  dx=")); Serial.print(dx);
    Serial.print(F(" dy=")); Serial.print(dy);
    Serial.println();
  }

  if (!joyAtCentre) {                                // waiting for the stick to come back
    if (abs(dx) < JOY_RELEASE && abs(dy) < JOY_RELEASE) joyAtCentre = true;
    return;
  }

  int8_t dir = -1;
  if (abs(dx) >= JOY_PUSH && abs(dx) > abs(dy)) dir = dx > 0 ? DIR_RIGHT : DIR_LEFT;
  else if (abs(dy) >= JOY_PUSH && abs(dy) > abs(dx)) dir = DIR_CENTER;   // up or down

  if (dir < 0) {                                     // resting: no push
    joyCandidate = -1;
    joyCandidateCount = 0;
    if (abs(dx) < JOY_RELEASE && abs(dy) < JOY_RELEASE) {   // follow slow drift
      joyCentreX = (joyCentreX * 15 + rawX) / 16;
      joyCentreY = (joyCentreY * 15 + rawY) / 16;
    }
    return;
  }

  if (dir == joyCandidate) joyCandidateCount++;
  else { joyCandidate = dir; joyCandidateCount = 1; }
  if (joyCandidateCount < JOY_CONFIRM_READS) return; // not confirmed yet

  joyAtCentre = false;                               // one event per push
  joyCandidate = -1;
  joyCandidateCount = 0;
  sendDirection(dir);
}

// =====================================================================================
// MICROPHONE - clap detector
// The swing (max - min) is measured over 30 ms windows. Above CLAP_THRESHOLD it keeps
// listening 120 ms for the peak, sends "CLAP" (Java slows the game down), then ignores
// the mic for 250 ms. The mic is also ignored while the buzzer plays.
// =====================================================================================
const uint8_t MIC_LISTEN = 0, MIC_PEAK = 1, MIC_IGNORE = 2;
uint8_t micState = MIC_LISTEN;
unsigned long micStart = 0;
int micLo = 1023, micHi = 0, micPeak = 0;

void micReset(unsigned long now) { micStart = now; micLo = 1023; micHi = 0; }

void updateMic() {
  unsigned long now = millis();

  if (soundPlaying || (long)(micQuietUntil - now) > 0) {   // our own buzzer is not a clap
    micState = MIC_LISTEN;
    micReset(now);
    return;
  }

  if (micState == MIC_IGNORE) {                      // skip the clap's echo
    if (now - micStart >= CLAP_IGNORE_MS) { micState = MIC_LISTEN; micReset(now); }
    return;
  }

  int v = analogRead(PIN_MIC);
  if (v < micLo) micLo = v;
  if (v > micHi) micHi = v;

  if (micState == MIC_LISTEN) {
    if (now - micStart >= CLAP_WINDOW_MS) {          // window finished
      int swing = micHi - micLo;
      micReset(now);
      if (swing > CLAP_THRESHOLD) { micPeak = swing; micState = MIC_PEAK; }
    }
  } else if (now - micStart >= CLAP_PEAK_MS) {       // MIC_PEAK finished
    int swing = micHi - micLo;
    if (swing > micPeak) micPeak = swing;
    sendEvent("CLAP");
    if (DEBUG_TO_USB) { Serial.print(F("  clap level ")); Serial.println(micPeak); }
    micState = MIC_IGNORE;
    micStart = now;
  }
}

// =====================================================================================
// SENDING EVENTS
// =====================================================================================
void sendEvent(const char *msg) {
  xbee.println(msg);
  if (DEBUG_TO_USB) Serial.println(msg);
}

void sendEventNum(const char *name, int value) {     // e.g. "BUTTON_PRESS,2"
  xbee.print(name); xbee.print(','); xbee.println(value);
  if (DEBUG_TO_USB) { Serial.print(name); Serial.print(','); Serial.println(value); }
}

// =====================================================================================
// RECEIVING COMMANDS (from the XBee, or typed in the Serial Monitor)
// =====================================================================================
LineBuffer xbeeLine = {{0}, 0};
LineBuffer usbLine  = {{0}, 0};

void handleCommand(char *line) {
  if (DEBUG_TO_USB) { Serial.print(F("CMD ")); Serial.println(line); }
  for (char *p = line; *p; p++) *p = toupper(*p);    // accept lower-case typing too
  char *cmd = strtok(line, ",");
  if (!cmd) return;
  char *a1 = strtok(NULL, ",");          // first argument (may be NULL)
  char *a2 = strtok(NULL, ",");          // second argument (may be NULL)
  int8_t led = a1 ? atoi(a1) - 1 : -1;   // LED number 1-3 -> index 0-2

  if (!strcmp(cmd, "LIGHT")) {                       // LIGHT,id,COLOR
    if (led >= 0 && led < 3 && a2) {
      sweeping = false;
      if (flickerLed == led) flickerLed = -1;
      ledColor[led] = colourFromWord(a2);
      ledDim[led] = false;
    }
  } else if (!strcmp(cmd, "FLICKER")) {              // FLICKER,id,COLOR
    if (led >= 0 && led < 3 && a2) startFlicker(led, colourFromWord(a2));
  } else if (!strcmp(cmd, "SCAN")) {                 // SCAN,step,duration
    if (a1 && a2) startSweep(atoi(a1), atoi(a2));
  } else if (!strcmp(cmd, "OFF")) {                  // OFF
    sweeping = false;
    flickerLed = -1;
    allOff();
  } else if (!strcmp(cmd, "SOUND")) {                // SOUND,TYPE,0
    playSoundByName(a1);
  } else if (!strcmp(cmd, "PING")) {                 // PING -> READY
    sendEvent("READY");
  }
}

// collects characters until a newline, then runs the command
void readCommands(Stream &in, LineBuffer &lb) {
  while (in.available()) {
    char ch = in.read();
    if (ch == '\n' || ch == '\r') {
      if (lb.len > 0) {
        lb.buf[lb.len] = '\0';
        handleCommand(lb.buf);
        lb.len = 0;
      }
    } else if (lb.len < sizeof(lb.buf) - 1) {
      lb.buf[lb.len++] = ch;
    }
  }
}

// =====================================================================================
// SETUP / LOOP
// =====================================================================================
void setup() {
  Serial.begin(9600);                    // FTDI: debugging / Serial Monitor
  xbee.begin(9600);                      // XBee: link to the Java program

  pinMode(PIN_LED_R, OUTPUT); digitalWrite(PIN_LED_R, HIGH);   // colours off
  pinMode(PIN_LED_G, OUTPUT); digitalWrite(PIN_LED_G, HIGH);
  pinMode(PIN_LED_B, OUTPUT); digitalWrite(PIN_LED_B, HIGH);
  for (uint8_t i = 0; i < 3; i++) plusOff(i);
  pinMode(PIN_BTN, INPUT);

  pinMode(PIN_IR_RX, INPUT_PULLUP);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_BUZZER2, OUTPUT);
  buzzerOff();
  pinMode(PIN_MIC, INPUT);

  calibrateJoystick();                   // do not touch the joystick while powering up
  irWinStart = millis();
  playSound(SOUND(SND_SUCCESS));         // bright beeps = board is alive
  sendEvent("READY");
}

void loop() {
  // everything is non-blocking, so all of these run hundreds of times per second
  updateMux();          // LEDs + push buttons
  updateSweep();        // red / green / blue scan moving to the left
  updateFlicker();      // fake flicker timing
  updateIrReceiver();   // gun beam -> SHOT,2 (stray IR ignored)
  updateJoystick();     // joystick directions + button
  updateMic();          // clap detection (slows the game down)
  updateSound();        // buzzer melodies
  readCommands(xbee, xbeeLine);    // commands from Java
  readCommands(Serial, usbLine);   // commands typed in the Serial Monitor
}
