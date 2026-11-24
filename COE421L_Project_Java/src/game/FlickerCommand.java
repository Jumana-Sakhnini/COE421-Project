package game;

/** "FLICKER,<id>,<COLOR>" - Level 3 trick: a DIM colour for 150 ms that must be ignored. */
public class FlickerCommand implements HardwareCommand {
    private final int targetID;
    private final Color color;

    public FlickerCommand(int targetID, Color color) { this.targetID = targetID; this.color = color; }

    @Override public void execute() { System.out.println("  (fake flicker on LED " + targetID + ")"); }
    @Override public String toProtocolMessage() { return "FLICKER," + targetID + "," + color; }
}
