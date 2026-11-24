package game;

/** RED: only a hit on the IR target (SHOT,2 from the IR receiver) counts. Buttons or the joystick are wrong. */
public class ShootValidation implements HitValidationStrategy {
    @Override
    public ValidationResult validate(Target target, HardwareMessage input) {
        if (input.getType() == MessageType.SHOT && input.getTargetId() == target.getId())
            return ValidationResult.CORRECT;
        return ValidationResult.WRONG;
    }
}
