package game;

import java.util.ArrayList;
import java.util.List;

/**
 * GREEN: the player repeats the pattern the LEDs showed, one joystick move per LED
 * (LED1 = LEFT, LED2 = CENTER = push up or down, LED3 = RIGHT).
 * Each correct move returns IN_PROGRESS until the last one returns CORRECT.
 * A wrong direction, a shot or a button press returns WRONG and the player starts the
 * pattern again from its first move (the round itself goes on).
 */
public class JoystickPatternValidation implements HitValidationStrategy {
    private final List<Position> pattern;
    private int index;                         // next move expected

    public JoystickPatternValidation(List<Position> pattern) {
        this.pattern = new ArrayList<>(pattern);
    }

    @Override
    public ValidationResult validate(Target target, HardwareMessage input) {
        if (input.getType() != MessageType.JOYSTICK || input.getPosition() != pattern.get(index)) {
            index = 0;                         // wrong: repeat the pattern from the start
            return ValidationResult.WRONG;
        }
        index++;
        return index == pattern.size() ? ValidationResult.CORRECT : ValidationResult.IN_PROGRESS;
    }

    public int remaining() { return pattern.size() - index; }
    public int getIndex() { return index; }               // moves already done
    public List<Position> getPattern() { return new ArrayList<>(pattern); }
}
