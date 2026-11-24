package game;

/**
 * The numbers that make each level different. GameManager reads the current
 * LevelConfig instead of using if-statements per level, so a level can be tuned
 * here without touching the game logic.
 *
 *  Level 1 - slow:            gun + buttons,             5 correct actions to pass
 *  Level 2 - a notch faster:  gun + buttons + joystick,  5 correct actions to pass
 *  Level 3 - endless:         everything + fake flickers; every correct action makes
 *                             it faster, until the officer loses the last heart.
 */
public class LevelConfig {
    public final int number;
    public final String name;
    final Color[] colours;        // which challenge colours can appear
    public final int goal;        // correct actions needed to pass (0 = endless)
    final int windowMs;           // reaction time at the start of the level
    final int windowSpeedUpMs;    // reaction time removed per correct action (level 3)
    final int minWindowMs;        // reaction time never goes below this
    final int scanStepMs;         // ms per LED of the red/green/blue sweep
    final int scanSpeedUpMs;      // sweep step removed per correct action (level 3)
    final int minScanStepMs;
    final int scanMinMs, scanMaxMs;   // random length of the sweep (at the level's start speed)
    final int patternLength;      // joystick moves in a green access code (0 = no green)
    final int patternOnMs;        // how long each green LED of the code stays on
    final int patternOffMs;       // gap between two code LEDs
    final int patternExtraMs;     // extra answer time per code move
    final double flickerChance;   // chance of a fake dim flicker before a challenge

    LevelConfig(int number, String name, Color[] colours, int goal,
                int windowMs, int windowSpeedUpMs, int minWindowMs,
                int scanStepMs, int scanSpeedUpMs, int minScanStepMs, int scanMinMs, int scanMaxMs,
                int patternLength, int patternOnMs, int patternOffMs, int patternExtraMs,
                double flickerChance) {
        this.number = number; this.name = name; this.colours = colours; this.goal = goal;
        this.windowMs = windowMs; this.windowSpeedUpMs = windowSpeedUpMs; this.minWindowMs = minWindowMs;
        this.scanStepMs = scanStepMs; this.scanSpeedUpMs = scanSpeedUpMs; this.minScanStepMs = minScanStepMs;
        this.scanMinMs = scanMinMs; this.scanMaxMs = scanMaxMs;
        this.patternLength = patternLength; this.patternOnMs = patternOnMs;
        this.patternOffMs = patternOffMs; this.patternExtraMs = patternExtraMs;
        this.flickerChance = flickerChance;
    }

    public boolean isEndless() { return goal == 0; }

    /** Reaction time after a number of correct actions in this level. */
    int window(int correct) { return Math.max(minWindowMs, windowMs - windowSpeedUpMs * correct); }

    /** Sweep step after a number of correct actions in this level. */
    int scanStep(int correct) { return Math.max(minScanStepMs, scanStepMs - scanSpeedUpMs * correct); }

    private static final Color[] GUN_BUTTONS = {Color.RED, Color.BLUE};
    private static final Color[] ALL = {Color.RED, Color.BLUE, Color.GREEN};

    public static final LevelConfig[] LEVELS = {
        //              no  name                      colours     goal  win  -win  min   step -st  min  scanMin scanMax len  on   off  +ms  flicker
        new LevelConfig(1, "PERIMETER PATROL",         GUN_BUTTONS, 5, 4000,   0, 4000,  400,  0, 400,  2500,  3500,  0,   0,   0,   0, 0.0),
        new LevelConfig(2, "ACCESS CONTROL",           ALL,         5, 3200,   0, 3200,  300,  0, 300,  2000,  3000,  3, 500, 200, 700, 0.0),
        new LevelConfig(3, "FULL LOCKDOWN",            ALL,         0, 2600, 150,  900,  220, 10, 100,  1500,  2200,  4, 380, 150, 600, 0.4),
    };
}
