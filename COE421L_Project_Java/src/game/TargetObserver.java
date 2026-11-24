package game;

/** Observer pattern: anything that reacts when a target round ends (score, sound). */
public interface TargetObserver {
    void onTargetHit(Target target, boolean success);
}
