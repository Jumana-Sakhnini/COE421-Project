package game;

/** Every kind of line the LilyPad can send. */
public enum MessageType {
    SHOT,            // "SHOT,2"            the IR receiver detected one shot from the gun
    BUTTON_PRESS,    // "BUTTON_PRESS,1"    push button 1-3
    JOYSTICK,        // "JOYSTICK,LEFT"     LEFT, CENTER (pushed up or down) or RIGHT
    CLAP,            // "CLAP"              microphone -> slows the game down
    READY,           // "READY"             LilyPad started
    SCAN_DONE,       // "SCAN_DONE"         colour scan finished (information only)
    UNKNOWN
}
