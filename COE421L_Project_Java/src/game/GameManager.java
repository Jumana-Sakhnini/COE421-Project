package game;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * GameManager - the "brain" of the Sentry Turret security game.
 *
 * DESIGN PATTERNS
 *  - Singleton : only one GameManager exists (getInstance()).
 *  - State     : the START SHIFT button and the inputs are handled by the current GameState
 *                (Setup, WaitingForOfficer, Playing, SessionOver).
 *  - Strategy  : the active Target judges each input with its HitValidationStrategy
 *                (ShootValidation, ButtonPressValidation, JoystickPatternValidation).
 *  - Observer  : a finished challenge notifies the Target's observers (ScoreBoard,
 *                SoundFeedback); the screen is a GameListener that gets a GameView.
 *  - Command   : every message to the LilyPad is a HardwareCommand object.
 *
 * THREADS
 *  - "Arduino-Serial-Listener" (CommunicationManager) delivers the LilyPad's lines.
 *  - "Game-Timer" runs every timed step (sweep, flicker, access code, time-outs).
 *  - The Swing thread draws the screen and sends the simulated inputs.
 *  All entry points are synchronized. Every timed step carries a "generation" number;
 *  when a challenge ends the generation changes and old steps do nothing.
 *
 * RULES
 *  The game asks for the number of security officers and their names. Each officer in
 *  turn presses START SHIFT on the screen to begin a shift. A shift only ends when the officer
 *  has lost all 3 hearts - nothing else (no button, sensor, clap or error) stops it.
 *
 *  RED   -> shoot the intruder at the middle target (IR receiver)     +15
 *  BLUE  -> press the push button under that LED (lock the breach)    +10
 *  GREEN -> watch the green access code, repeat it with the joystick  +20
 *           (LED 1 = LEFT, LED 2 = CENTER (up or down), LED 3 = RIGHT)
 *  3rd correct action in a row and later: +5 combo bonus.
 *
 *  A heart is lost for: a wrong action (the same challenge stays up, an access code
 *  starts again), a time-out (the next challenge comes), or reacting to a fake
 *  flicker (level 3). Inputs that cannot be meant for the challenge are ignored:
 *  a SHOT when no red target is up (stray IR), a joystick move when no code is asked.
 *
 *  Level 1 (slow) and Level 2 (a notch faster, adds the joystick) need 5 correct
 *  actions each. Level 3 is endless: every correct action makes it faster.
 *  A CLAP slows the game down (+700 ms reaction time, slower sweep, max +2100 ms);
 *  in level 3 each correct action takes 100 ms of that bonus away again.
 *
 *  After the last officer, the session leaderboard and the all-time top 5 are shown.
 */
public class GameManager {

    // ------------------------------------------------------------------ Singleton
    private static GameManager instance;

    public static synchronized GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }

    // ------------------------------------------------------------------ constants
    public static final int IR_TARGET_ID = 2;       // the IR receiver sits under the middle LED
    public static final int MAX_HEARTS = 3;
    private static final int START_DELAY_MS = 1800;      // start sound + "LEVEL 1" banner
    private static final int LEVEL_DELAY_MS = 2600;      // pause/banner between levels
    private static final int RESULT_MS = 900;            // "SECURED"/"FAILED" before the next challenge
    private static final int DOWN_MS = 3500;             // "OFFICER DOWN" screen
    private static final int FLICKER_MS = 150;           // must match the LilyPad's FLICKER_MS
    private static final int DECOY_TRAP_MS = 700;        // acting during this time costs a heart
    private static final int CLAP_SLOW_MS = 700;
    private static final int CLAP_MAX_MS = 2100;
    private static final int CLAP_DECAY_MS = 100;        // level 3: bonus lost per correct action
    private static final int CLAP_COOLDOWN_MS = 600;     // one clap is often reported twice

    // ------------------------------------------------------------------ fields
    private GameState state = new SetupState();
    private final Map<Integer, Target> targets = new HashMap<>();
    private final ScoreBoard scoreBoard = new ScoreBoard();
    private final Leaderboard leaderboard = new Leaderboard("leaderboard.txt");
    private final CommunicationManager comm;
    private final Random random = new Random();
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();
    private final Deque<String> log = new ArrayDeque<>();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Game-Timer");
        t.setDaemon(true);
        return t;
    });

    private final List<Officer> officers = new ArrayList<>();
    private int current;                     // index of the officer on duty / next
    private int hearts;
    private LevelConfig level = LevelConfig.LEVELS[0];
    private int levelIndex, levelCorrect, totalCorrect;
    private int slowMs;
    private long lastClap, lastWrong;

    private ZoneMode mode = ZoneMode.IDLE;
    private long modeStart;
    private String banner = "";
    private Target activeTarget;
    private boolean waiting;                 // a reaction window is open
    private int decoyLed;
    private Color decoyColor = Color.OFF;
    private int patternShowIndex = -1;
    private int patternRevealed;             // code steps shown so far
    private int resultLed;                   // zone of the last SECURED / FAILED result
    private Color resultColor = Color.OFF;
    private long deadline;
    private int windowMs, scanStepMs, scanDurationMs;
    private int generation;
    private ScheduledFuture<?> timeout;

    private GameManager() {
        String port = System.getProperty("game.serial.port", "COM8");
        comm = new CommunicationManager(port, 9600);
        for (int id = 1; id <= 3; id++) {
            Target t = new Target(id, id == 1 ? Position.LEFT : id == 2 ? Position.CENTER : Position.RIGHT);
            t.addObserver(scoreBoard);                              // Observer 1: points
            t.addObserver(new SoundFeedback(comm, scoreBoard));     // Observer 2: sounds
            targets.put(id, t);
        }
    }

    // ------------------------------------------------------------------ public API
    public CommunicationManager getCommunicationManager() { return comm; }

    public void addListener(GameListener l) { listeners.add(l); l.gameChanged(snapshot()); }

    /** Starts a session with these officers (in turn order). */
    public synchronized void setOfficers(List<String> names) {
        if (names == null || names.isEmpty()) return;
        cancelPending();
        officers.clear();
        for (String n : names) officers.add(new Officer(n));
        current = 0;
        log("Security team on duty: " + officers.size() + " officer(s)");
        callNextOfficer();
    }

    /** Called by the serial listener (or the on-screen simulator) for every line. */
    public synchronized void handleHardwareMessage(String line) {
        try {
            HardwareMessage msg = HardwareMessage.parse(line);
            switch (msg.getType()) {
                case READY:     log("LilyPad online (READY)"); break;
                case SCAN_DONE: case UNKNOWN: break;
                default:        state.onInput(this, msg);
            }
        } catch (RuntimeException e) {                 // a bad message never stops the game
            log("(could not handle \"" + line + "\": " + e + ")");
        }
        notifyListeners();
    }

    /** The START SHIFT / NEW SHIFT button on the screen. */
    public synchronized void startShiftPressed() {
        try {
            state.startShift(this);
        } catch (RuntimeException e) {
            log("(could not start the shift: " + e + ")");
        }
        notifyListeners();
    }

    public synchronized boolean isPlaying() { return state instanceof PlayingState; }
    public synchronized boolean isSessionOver() { return state instanceof SessionOverState; }
    public synchronized List<String> getOfficerNames() {
        List<String> n = new ArrayList<>();
        for (Officer o : officers) n.add(o.getName());
        return n;
    }

    public synchronized void shutdown() {
        cancelPending();
        timer.shutdownNow();
        comm.sendCommand(new AllOffCommand());
    }

    // ------------------------------------------------------------------ called by the states
    void setState(GameState s) { state = s; }

    /** WaitingForOfficerState: START SHIFT begins the shift. */
    void beginShift() {
        cancelPending();
        Officer o = officers.get(current);
        scoreBoard.reset();
        hearts = MAX_HEARTS;
        levelIndex = 0;
        level = LevelConfig.LEVELS[0];
        levelCorrect = 0;
        totalCorrect = 0;
        slowMs = 0;
        setState(new PlayingState());
        comm.sendCommand(new AllOffCommand());
        comm.sendCommand(new PlaySoundCommand(SoundType.START, 0));
        log("SHIFT STARTED - " + o.getName());
        showBanner("LEVEL 1 - " + level.name);
        schedule(this::nextChallenge, START_DELAY_MS);
    }

    /** SessionOverState: NEW SHIFT plays again with the same team. */
    void restartSession() {
        for (Officer o : officers) o.reset();
        current = 0;
        log("New shift round for the same team");
        callNextOfficer();
    }

    /** PlayingState: SHOT / BUTTON_PRESS / JOYSTICK / CLAP. */
    void processInput(HardwareMessage msg) {
        if (msg.getType() == MessageType.CLAP) { slowDown(); return; }
        if (mode == ZoneMode.OFFICER_DOWN) return;

        if (mode == ZoneMode.DECOY) {                       // acted on a fake signal
            if (msg.getType() == MessageType.JOYSTICK) return;   // stick bumps are not "acting"
            log("DECOY! " + msg.describe() + " reacted to a ghost signal");
            loseHeart();
            return;
        }
        if (!waiting || activeTarget == null) {             // sweep / code display / pauses
            if (msg.getType() == MessageType.SHOT) log("(stray IR ignored)");
            return;
        }
        Color c = activeTarget.getCurrentColor();
        // inputs that cannot be meant for this challenge are ignored (noise filter)
        if (msg.getType() == MessageType.SHOT && c != Color.RED) { log("(stray IR ignored - no intruder)"); return; }
        if (msg.getType() == MessageType.JOYSTICK && c != Color.GREEN) return;

        ValidationResult r = activeTarget.validate(msg);    // Strategy decides
        switch (r) {
            case CORRECT:
                correct(msg);
                break;
            case IN_PROGRESS:
                comm.sendCommand(new PlaySoundCommand(SoundType.TICK, 0));
                log("Code " + msg.getPosition() + " accepted - "
                        + ((JoystickPatternValidation) activeTarget.getStrategy()).remaining() + " to go");
                break;
            case WRONG:
                log("WRONG: " + msg.describe() + (c == Color.GREEN ? " - code reset, enter it again" : " - try again"));
                loseHeart();
                break;
            default:
        }
    }

    // ------------------------------------------------------------------ challenge flow
    private void nextChallenge() {
        if (!level.isEndless() && levelCorrect >= level.goal) {     // level passed
            levelIndex++;
            level = LevelConfig.LEVELS[levelIndex];
            levelCorrect = 0;
            comm.sendCommand(new PlaySoundCommand(SoundType.LEVEL_UP, 0));
            log("LEVEL " + level.number + " - " + level.name);
            showBanner("LEVEL " + level.number + " - " + level.name);
            schedule(this::nextChallenge, LEVEL_DELAY_MS);
            return;
        }
        // 1. the red -> green -> blue sweep moving to the left
        windowMs = level.window(levelCorrect) + slowMs;
        scanStepMs = level.scanStep(levelCorrect) + slowMs / 20;
        int scanBase = level.scanMinMs + random.nextInt(level.scanMaxMs - level.scanMinMs + 1);
        scanDurationMs = Math.max(900, scanBase * scanStepMs / level.scanStepMs);
        comm.sendCommand(new ScanCommand(scanStepMs, scanDurationMs));
        setMode(ZoneMode.SCANNING);
        schedule(this::afterScan, scanDurationMs + 120);
    }

    /** 2. Level 3: sometimes a fake dim flicker comes first. */
    private void afterScan() {
        if (random.nextDouble() < level.flickerChance) {
            decoyLed = 1 + random.nextInt(3);
            decoyColor = level.colours[random.nextInt(level.colours.length)];
            comm.sendCommand(new FlickerCommand(decoyLed, decoyColor));
            setMode(ZoneMode.DECOY);
            log("Faint signal on LED " + decoyLed + " ...");
            schedule(() -> { decoyLed = 0; decoyColor = Color.OFF; showChallenge(); },
                     FLICKER_MS + DECOY_TRAP_MS);
        } else {
            showChallenge();
        }
    }

    /** 3. A red, blue or green challenge. */
    private void showChallenge() {
        Color colour = level.colours[random.nextInt(level.colours.length)];
        if (colour == Color.RED) {
            activeTarget = targets.get(IR_TARGET_ID);
            activeTarget.activate(Color.RED, new ShootValidation());
            comm.sendCommand(new LightTargetCommand(IR_TARGET_ID, Color.RED));
            log("INTRUDER in ZONE B - SHOOT! (" + windowMs + " ms)");
            openWindow(windowMs);
        } else if (colour == Color.BLUE) {
            int id = 1 + random.nextInt(3);
            activeTarget = targets.get(id);
            activeTarget.activate(Color.BLUE, new ButtonPressValidation());
            comm.sendCommand(new LightTargetCommand(id, Color.BLUE));
            log("BREACH in ZONE " + zone(id) + " - press button " + id + " (" + windowMs + " ms)");
            openWindow(windowMs);
        } else {
            List<Position> code = new ArrayList<>();
            Position[] options = Position.values();
            for (int i = 0; i < level.patternLength; i++) code.add(options[random.nextInt(3)]);
            activeTarget = targets.get(IR_TARGET_ID);          // the code belongs to the whole row
            activeTarget.activate(Color.GREEN, new JoystickPatternValidation(code));
            patternRevealed = 0;
            log("ACCESS CODE incoming - watch the green lights");
            setMode(ZoneMode.CODE_SHOW);
            showCodeStep(code, 0);
        }
    }

    /** Lights the code one LED at a time (LED1 = LEFT, LED2 = CENTER, LED3 = RIGHT). */
    private void showCodeStep(List<Position> code, int index) {
        if (index >= code.size()) {
            patternShowIndex = -1;
            comm.sendCommand(new AllOffCommand());
            int time = windowMs + level.patternExtraMs * code.size();
            log("Enter the " + code.size() + "-move code (" + time + " ms)");
            openWindow(time);
            return;
        }
        int onMs = Math.max(200, level.patternOnMs - (level.isEndless() ? 10 * levelCorrect : 0));
        patternShowIndex = index;
        patternRevealed = index + 1;
        comm.sendCommand(new LightTargetCommand(led(code.get(index)), Color.GREEN));
        notifyListeners();
        schedule(() -> {
            patternShowIndex = -1;
            comm.sendCommand(new LightTargetCommand(led(code.get(index)), Color.OFF));
            notifyListeners();
            schedule(() -> showCodeStep(code, index + 1), level.patternOffMs);
        }, onMs);
    }

    /** 4. The reaction window. */
    private void openWindow(int ms) {
        waiting = true;
        windowMs = ms;
        setMode(ZoneMode.RESPOND);
        armTimeout(ms);
    }

    private void armTimeout(long ms) {
        if (timeout != null) timeout.cancel(false);
        deadline = System.currentTimeMillis() + ms;
        int gen = generation;
        timeout = timer.schedule(() -> {
            synchronized (GameManager.this) {
                if (gen == generation && waiting) safely(this::timeUp);
                notifyListeners();
            }
        }, ms, TimeUnit.MILLISECONDS);
    }

    private void rememberResult() {
        resultLed = activeTarget.getCurrentColor() == Color.GREEN ? 0 : activeTarget.getId();
        resultColor = activeTarget.getCurrentColor();
    }

    private void correct(HardwareMessage msg) {
        endWindow();
        rememberResult();
        comm.sendCommand(new AllOffCommand());
        activeTarget.complete(true);                     // Observer: ScoreBoard + SoundFeedback
        levelCorrect++;
        totalCorrect++;
        if (level.isEndless()) slowMs = Math.max(0, slowMs - CLAP_DECAY_MS);
        log("SECURED (" + msg.describe() + ") +" + scoreBoard.getLastPoints()
                + (scoreBoard.isCombo() ? " COMBO x" + scoreBoard.getStreak() : ""));
        setMode(ZoneMode.SECURED);
        schedule(this::nextChallenge, RESULT_MS);
    }

    private void timeUp() {
        endWindow();
        rememberResult();
        comm.sendCommand(new AllOffCommand());
        activeTarget.complete(false);                    // streak reset + failure sound
        log("TOO SLOW - zone not secured");
        hearts--;
        lastWrong = System.currentTimeMillis();
        if (hearts <= 0) { officerDown(); return; }
        setMode(ZoneMode.FAILED);
        schedule(this::nextChallenge, RESULT_MS + 300);
    }

    /** Wrong action or decoy: one heart; the challenge stays up. */
    private void loseHeart() {
        hearts--;
        lastWrong = System.currentTimeMillis();
        scoreBoard.resetStreak();
        if (hearts <= 0) { officerDown(); return; }
        comm.sendCommand(new PlaySoundCommand(SoundType.FAILURE, 0));
        log(hearts + " heart" + (hearts == 1 ? "" : "s") + " left");
    }

    private void officerDown() {
        cancelPending();
        hearts = 0;
        for (Target t : targets.values()) t.deactivate();
        activeTarget = null;
        comm.sendCommand(new AllOffCommand());
        comm.sendCommand(new PlaySoundCommand(SoundType.DEAD, 0));
        Officer o = officers.get(current);
        o.finishShift(scoreBoard.getScore(), level.number);
        leaderboard.add(o.getName(), o.getScore());
        log("OFFICER DOWN - " + o.getName() + " scored " + o.getScore() + " (level " + level.number + ")");
        setMode(ZoneMode.OFFICER_DOWN);
        int gen = generation;
        timer.schedule(() -> {
            synchronized (GameManager.this) {
                if (gen != generation) return;
                safely(() -> {
                    current++;
                    if (current < officers.size()) callNextOfficer();
                    else endSession();
                });
                notifyListeners();
            }
        }, DOWN_MS, TimeUnit.MILLISECONDS);
    }

    private void callNextOfficer() {
        setState(new WaitingForOfficerState());
        setMode(ZoneMode.IDLE);
        Officer o = officers.get(current);
        log("Officer " + (current + 1) + "/" + officers.size() + ": " + o.getName()
                + " - press START SHIFT on the screen");
        notifyListeners();
    }

    private void endSession() {
        setState(new SessionOverState());
        setMode(ZoneMode.IDLE);
        comm.sendCommand(new PlaySoundCommand(SoundType.SESSION_END, 0));
        List<Officer> ranking = sessionRanking();
        log("ALL SHIFTS COMPLETE - top officer: " + ranking.get(0).getName() + " (" + ranking.get(0).getScore() + ")");
        current = 0;
        notifyListeners();
    }

    /** CLAP: slows the game down. */
    private void slowDown() {
        long now = System.currentTimeMillis();
        if (now - lastClap < CLAP_COOLDOWN_MS) return;
        lastClap = now;
        if (slowMs >= CLAP_MAX_MS) { log("CLAP - already at the slowest speed"); return; }
        int add = Math.min(CLAP_SLOW_MS, CLAP_MAX_MS - slowMs);
        slowMs += add;
        comm.sendCommand(new PlaySoundCommand(SoundType.SLOW, 0));
        if (waiting) {                                       // more time for this challenge too
            long left = Math.max(0, deadline - now);
            windowMs += add;
            armTimeout(left + add);
        }
        log("CLAP - game slowed down (+" + slowMs + " ms reaction time)");
    }

    // ------------------------------------------------------------------ helpers
    private void endWindow() {
        waiting = false;
        generation++;                                        // cancels the pending time-out
        if (timeout != null) timeout.cancel(false);
        deadline = 0;
    }

    private void cancelPending() {
        waiting = false;
        generation++;
        if (timeout != null) timeout.cancel(false);
        deadline = 0;
        patternShowIndex = -1;
        decoyLed = 0;
    }

    private void showBanner(String text) { banner = text; setMode(ZoneMode.BANNER); }

    private void setMode(ZoneMode m) {
        mode = m;
        modeStart = System.currentTimeMillis();
        notifyListeners();
    }

    /** Runs a step later on the timer thread, unless something cancelled it meanwhile. */
    private void schedule(Runnable step, int delayMs) {
        int gen = generation;
        timer.schedule(() -> {
            synchronized (GameManager.this) {
                if (gen == generation && state instanceof PlayingState) safely(step);
                notifyListeners();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /** An unexpected error inside a step never stops the shift: the next challenge comes. */
    private void safely(Runnable step) {
        try {
            step.run();
        } catch (RuntimeException e) {
            log("(internal error: " + e + " - continuing)");
            cancelPending();
            for (Target t : targets.values()) t.deactivate();
            comm.sendCommand(new AllOffCommand());
            if (state instanceof PlayingState) schedule(this::nextChallenge, RESULT_MS);
        }
    }

    synchronized void log(String text) {
        String line = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  " + text;
        System.out.println(line);
        log.addFirst(line);
        while (log.size() > 60) log.removeLast();
    }

    private List<Officer> sessionRanking() {
        List<Officer> r = new ArrayList<>();
        for (Officer o : officers) if (o.isDone()) r.add(o);
        r.sort((a, b) -> b.getScore() != a.getScore() ? b.getScore() - a.getScore()
                : b.getLevelReached() - a.getLevelReached());
        return r;
    }

    private static int led(Position p) { return p == Position.LEFT ? 1 : p == Position.CENTER ? 2 : 3; }
    private static String zone(int id) { return id == 1 ? "A" : id == 2 ? "B" : "C"; }

    private void notifyListeners() {
        if (listeners.isEmpty()) return;
        GameView v = snapshot();
        for (GameListener l : listeners) l.gameChanged(v);
    }

    private synchronized GameView snapshot() {
        GameView.Builder b = new GameView.Builder();
        b.stateName = state.getName();
        b.mode = mode;
        b.connected = comm.isConnected();
        if (!officers.isEmpty()) {
            Officer o = officers.get(Math.min(current, officers.size() - 1));
            b.officerName = o.getName();
            b.officerNumber = Math.min(current, officers.size() - 1) + 1;
            b.officerCount = officers.size();
        }
        b.hearts = state instanceof PlayingState ? hearts : MAX_HEARTS;
        b.maxHearts = MAX_HEARTS;
        b.score = state instanceof PlayingState ? scoreBoard.getScore() : 0;
        b.streak = scoreBoard.getStreak();
        b.lastPoints = scoreBoard.getLastPoints();
        b.combo = scoreBoard.isCombo();
        b.level = level.number;
        b.levelName = level.name;
        b.levelCorrect = levelCorrect;
        b.levelGoal = level.goal;
        b.totalCorrect = totalCorrect;
        if (activeTarget != null && activeTarget.isActive()) {
            b.challengeColor = activeTarget.getCurrentColor();
            b.challengeLed = activeTarget.getId();
            if (activeTarget.getStrategy() instanceof JoystickPatternValidation) {
                JoystickPatternValidation j = (JoystickPatternValidation) activeTarget.getStrategy();
                b.pattern = j.getPattern();
                b.patternDone = j.getIndex();
            }
        }
        b.patternShowIndex = patternShowIndex;
        b.patternRevealed = patternRevealed;
        b.resultLed = resultLed;
        b.resultColor = resultColor;
        b.decoyLed = decoyLed;
        b.decoyColor = decoyColor;
        b.modeStart = modeStart;
        b.deadline = waiting ? deadline : 0;
        b.windowMs = windowMs;
        b.scanStepMs = scanStepMs;
        b.scanDurationMs = scanDurationMs;
        b.slowMs = slowMs;
        b.lastClap = lastClap;
        b.lastWrong = lastWrong;
        b.banner = banner;
        List<String[]> results = new ArrayList<>();
        for (Officer o : sessionRanking())
            results.add(new String[]{o.getName(), String.valueOf(o.getScore()), String.valueOf(o.getLevelReached())});
        b.sessionResults = results;
        b.allTimeTop = leaderboard.topEntries(5);
        b.officers = getOfficerNames();
        b.log = new ArrayList<>(log);
        return b.build();
    }
}
