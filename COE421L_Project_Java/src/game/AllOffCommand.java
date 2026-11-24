package game;

/** "OFF" - all LEDs off and any scan / flicker stopped (one short line instead of three). */
public class AllOffCommand implements HardwareCommand {
    @Override public void execute() { }
    @Override public String toProtocolMessage() { return "OFF"; }
}
