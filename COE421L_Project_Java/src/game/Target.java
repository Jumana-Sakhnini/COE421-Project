package game;

import java.util.ArrayList;
import java.util.List;

/**
 * One of the three physical targets (LED + push button; target 2 also has the IR
 * receiver). It holds its current colour and validation Strategy, and it is the
 * Observer subject: when a round ends it tells every TargetObserver the result.
 */
public class Target {
    private final int id;
    private final Position position;
    private Color currentColor = Color.OFF;
    private HitValidationStrategy hitStrategy;
    private final List<TargetObserver> observers = new ArrayList<>();
    private boolean active;

    public Target(int id, Position position) {
        this.id = id;
        this.position = position;
    }

    /** Lights this target in a colour with the rule (Strategy) that clears it. */
    public void activate(Color color, HitValidationStrategy strategy) {
        this.currentColor = color;
        this.hitStrategy = strategy;
        this.active = true;
    }

    public void deactivate() {
        active = false;
        currentColor = Color.OFF;
    }

    /** Asks the current Strategy whether this input is right. */
    public ValidationResult validate(HardwareMessage input) {
        if (!active || hitStrategy == null) return ValidationResult.IGNORED;
        return hitStrategy.validate(this, input);
    }

    /** End of the round: notify the observers (ScoreBoard, SoundFeedback), then switch off. */
    public void complete(boolean success) {
        for (TargetObserver o : observers) o.onTargetHit(this, success);
        deactivate();
    }

    public void addObserver(TargetObserver observer) {
        if (observer != null && !observers.contains(observer)) observers.add(observer);
    }

    public int getId() { return id; }
    public Position getPosition() { return position; }
    public Color getCurrentColor() { return currentColor; }
    public HitValidationStrategy getStrategy() { return hitStrategy; }
    public boolean isActive() { return active; }
}
