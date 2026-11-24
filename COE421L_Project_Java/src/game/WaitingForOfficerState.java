package game;

/** The next officer is called; the START SHIFT button on the screen starts their shift. */
public class WaitingForOfficerState implements GameState {
    @Override public void startShift(GameManager game) { game.beginShift(); }
    @Override public void onInput(GameManager game, HardwareMessage msg) {
        if (msg.getType() != MessageType.CLAP) game.log("(" + msg.describe() + " ignored - press START SHIFT on the screen)");
    }
    @Override public String getName() { return "WAITING FOR OFFICER"; }
}
