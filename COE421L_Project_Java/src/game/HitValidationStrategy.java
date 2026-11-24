package game;

/**
 * Strategy pattern: each target colour has its own rule for which input is right.
 * The Target simply asks its strategy, so GameManager needs no colour if-chain and
 * a new kind of target only needs a new strategy class.
 */
public interface HitValidationStrategy {
    ValidationResult validate(Target target, HardwareMessage input);
}
