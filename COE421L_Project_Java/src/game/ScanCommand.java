package game;

/**
 * "SCAN,<stepMs>,<durationMs>" - one LED at a time moves toward the LEFT
 * (LED 3 -> 2 -> 1 -> 3 ...), changing colour every step: RED -> GREEN -> BLUE.
 * The LilyPad sends SCAN_DONE at the end. The screen draws the same sequence.
 */
public class ScanCommand implements HardwareCommand {
    private final int stepMs, durationMs;

    public ScanCommand(int stepMs, int durationMs) { this.stepMs = stepMs; this.durationMs = durationMs; }

    @Override public void execute() { }
    @Override public String toProtocolMessage() { return "SCAN," + stepMs + "," + durationMs; }
}
