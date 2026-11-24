package game;

/** Before the officers are entered: nothing reacts. */
public class SetupState implements GameState {
    @Override public void startShift(GameManager game) { game.log("Enter the security officers first."); }
    @Override public void onInput(GameManager game, HardwareMessage msg) { }
    @Override public String getName() { return "SETUP"; }
}
