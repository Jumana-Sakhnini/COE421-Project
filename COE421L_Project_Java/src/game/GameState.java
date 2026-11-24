package game;

/**
 * State pattern: what the START SHIFT button and the player's inputs do depends on
 * the current state object.
 */
public interface GameState {
    /** The START SHIFT / NEW SHIFT button on the screen. */
    void startShift(GameManager game);
    /** SHOT, BUTTON_PRESS, JOYSTICK or CLAP. */
    void onInput(GameManager game, HardwareMessage msg);
    String getName();
}
