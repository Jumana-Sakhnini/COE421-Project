package game;

/**
 * Command pattern: every instruction to the LilyPad is an object that knows how to
 * describe itself (execute) and how to turn itself into one protocol line.
 */
public interface HardwareCommand {
    void execute();                 // log / local action before sending
    String toProtocolMessage();     // the text line sent over the XBee
}
