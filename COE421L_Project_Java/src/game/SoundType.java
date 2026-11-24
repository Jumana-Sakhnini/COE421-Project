package game;

/** Sounds the LilyPad's buzzer can play ("SOUND,<TYPE>,0"). */
public enum SoundType {
    START,        // shift starts
    SUCCESS,      // correct answer: bright and loud
    COMBO,        // correct answer, 3 or more in a row
    FAILURE,      // wrong answer / time-out / decoy: low, disapproving
    TICK,         // one correct joystick move of an access code
    SLOW,         // clap: game slowed down
    LEVEL_UP,     // next level
    DEAD,         // officer lost the last heart
    SESSION_END   // all officers done: leaderboard
}
