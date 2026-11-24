package game;

/**
 * Observer: gives points when a challenge is answered correctly.
 * RED +15, BLUE +10, GREEN access code +20, and from the 3rd correct answer in a row
 * a +5 combo bonus. A failed challenge (time-out) only resets the streak; losing
 * hearts is handled by the GameManager.
 */
public class ScoreBoard implements TargetObserver {
    public static final int COMBO_START = 3, COMBO_BONUS = 5;

    private int score;
    private int streak;
    private int lastPoints;

    @Override
    public synchronized void onTargetHit(Target target, boolean success) {
        if (!success) { streak = 0; lastPoints = 0; return; }
        int points = target.getCurrentColor() == Color.RED ? 15
                   : target.getCurrentColor() == Color.BLUE ? 10 : 20;
        streak++;
        if (streak >= COMBO_START) points += COMBO_BONUS;
        score += points;
        lastPoints = points;
    }

    public synchronized boolean isCombo() { return streak >= COMBO_START; }
    public synchronized int getScore() { return score; }
    public synchronized int getStreak() { return streak; }
    public synchronized int getLastPoints() { return lastPoints; }
    public synchronized void resetStreak() { streak = 0; }
    public synchronized void reset() { score = 0; streak = 0; lastPoints = 0; }
}
