package game;

/** What a HitValidationStrategy decides about one input. */
public enum ValidationResult {
    CORRECT,      // the target is cleared
    IN_PROGRESS,  // right so far (one correct joystick move of a longer pattern)
    WRONG,        // wrong input: the round is lost
    IGNORED       // nothing to do with this target (e.g. a missed shot)
}
