package game;

/** A security officer (player): name, and the result of their last shift. */
public class Officer {
    private final String name;
    private int score;
    private int levelReached = 1;
    private boolean done;            // has finished a shift in this session

    public Officer(String name) {
        this.name = (name == null || name.isBlank()) ? "Officer" : name.trim();
    }

    public String getName() { return name; }
    public int getScore() { return score; }
    public int getLevelReached() { return levelReached; }
    public boolean isDone() { return done; }

    void finishShift(int score, int levelReached) {
        this.score = score;
        this.levelReached = levelReached;
        this.done = true;
    }

    void reset() { score = 0; levelReached = 1; done = false; }
}
