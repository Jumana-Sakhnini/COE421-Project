package game;

/** BLUE: only the push button under the lit LED counts. Anything else is wrong. */
public class ButtonPressValidation implements HitValidationStrategy {
    @Override
    public ValidationResult validate(Target target, HardwareMessage input) {
        if (input.getType() == MessageType.BUTTON_PRESS && input.getTargetId() == target.getId())
            return ValidationResult.CORRECT;
        return ValidationResult.WRONG;
    }
}
