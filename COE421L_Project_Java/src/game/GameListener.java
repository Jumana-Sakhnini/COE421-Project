package game;

/** Observer for the screen: the GameManager calls this whenever something changes. */
public interface GameListener {
    void gameChanged(GameView view);
}
