package game;

import java.util.List;

/**
 * An immutable snapshot of the game, handed to the screen (GameListener). The screen
 * only reads it, so the game thread and the Swing thread never share mutable data.
 */
public final class GameView {
    public final String stateName;
    public final ZoneMode mode;
    public final boolean connected;

    // officer
    public final String officerName;
    public final int officerNumber, officerCount;
    public final int hearts, maxHearts;
    public final int score, streak, lastPoints;
    public final boolean combo;

    // level
    public final int level;
    public final String levelName;
    public final int levelCorrect, levelGoal;     // goal 0 = endless
    public final int totalCorrect;

    // challenge
    public final Color challengeColor;            // RED / BLUE / GREEN, or OFF
    public final int challengeLed;                // 1-3, 0 = none
    public final List<Position> pattern;          // green access code (empty otherwise)
    public final int patternDone;                 // moves already entered
    public final int patternShowIndex;            // code step being shown, -1 = none
    public final int patternRevealed;             // code steps shown so far
    public final int decoyLed;
    public final int resultLed;                   // zone of the last result, 0 = whole row (code)
    public final Color resultColor;
    public final Color decoyColor;

    // timing (System.currentTimeMillis based)
    public final long modeStart;
    public final long deadline;                   // reaction window end, 0 = none
    public final int windowMs;                    // length of the reaction window
    public final int scanStepMs;
    public final int scanDurationMs;
    public final int slowMs;                      // extra time from claps
    public final long lastClap;
    public final long lastWrong;

    public final String banner;
    public final List<String[]> sessionResults;   // {name, score, level} sorted best first
    public final List<String[]> allTimeTop;       // {name, score}
    public final List<String> officers;
    public final List<String> log;

    GameView(Builder b) {
        stateName = b.stateName; mode = b.mode; connected = b.connected;
        officerName = b.officerName; officerNumber = b.officerNumber; officerCount = b.officerCount;
        hearts = b.hearts; maxHearts = b.maxHearts; score = b.score; streak = b.streak;
        lastPoints = b.lastPoints; combo = b.combo;
        level = b.level; levelName = b.levelName; levelCorrect = b.levelCorrect; levelGoal = b.levelGoal;
        totalCorrect = b.totalCorrect;
        challengeColor = b.challengeColor; challengeLed = b.challengeLed;
        pattern = List.copyOf(b.pattern); patternDone = b.patternDone; patternShowIndex = b.patternShowIndex; patternRevealed = b.patternRevealed;
        decoyLed = b.decoyLed; decoyColor = b.decoyColor;
        resultLed = b.resultLed; resultColor = b.resultColor;
        modeStart = b.modeStart; deadline = b.deadline; windowMs = b.windowMs;
        scanStepMs = b.scanStepMs; scanDurationMs = b.scanDurationMs; slowMs = b.slowMs;
        lastClap = b.lastClap; lastWrong = b.lastWrong;
        banner = b.banner;
        sessionResults = List.copyOf(b.sessionResults); allTimeTop = List.copyOf(b.allTimeTop);
        officers = List.copyOf(b.officers); log = List.copyOf(b.log);
    }

    /** Filled in by the GameManager. */
    static class Builder {
        String stateName = "", officerName = "", levelName = "", banner = "";
        ZoneMode mode = ZoneMode.IDLE;
        boolean connected, combo;
        int officerNumber, officerCount, hearts, maxHearts, score, streak, lastPoints;
        int level = 1, levelCorrect, levelGoal, totalCorrect;
        Color challengeColor = Color.OFF, decoyColor = Color.OFF, resultColor = Color.OFF;
        int resultLed, challengeLed, patternDone, patternRevealed, patternShowIndex = -1, decoyLed;
        List<Position> pattern = List.of();
        long modeStart, deadline, lastClap, lastWrong;
        int windowMs, scanStepMs, scanDurationMs, slowMs;
        List<String[]> sessionResults = List.of(), allTimeTop = List.of();
        List<String> officers = List.of(), log = List.of();
        GameView build() { return new GameView(this); }
    }
}
