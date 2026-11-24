package game;

/** "LIGHT,<id>,<COLOR>" - set one LED to RED, BLUE, GREEN, WHITE or OFF. */
public class LightTargetCommand implements HardwareCommand {
    private final int targetID;
    private final Color color;

    public LightTargetCommand(int targetID, Color color) { this.targetID = targetID; this.color = color; }

    @Override public void execute() { }
    @Override public String toProtocolMessage() { return "LIGHT," + targetID + "," + color; }
}
