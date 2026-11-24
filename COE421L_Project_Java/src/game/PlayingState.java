package game;

/**
 * A shift is running. Only losing the last heart ends it: buttons,
 * wrong buttons, stray IR or noise never stop the game.
 */
public class PlayingState implements GameState {
    @Override public void startShift(GameManager game) { /* already on duty */ }
    @Override public void onInput(GameManager game, HardwareMessage msg) { game.processInput(msg); }
    @Override public String getName() { return "ON DUTY"; }
}
