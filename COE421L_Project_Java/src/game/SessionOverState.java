package game;

/** Every officer has finished: the leaderboard is shown. NEW SHIFT starts a new round with the same team. */
public class SessionOverState implements GameState {
    @Override public void startShift(GameManager game) { game.restartSession(); }
    @Override public void onInput(GameManager game, HardwareMessage msg) { }
    @Override public String getName() { return "LEADERBOARD"; }
}
