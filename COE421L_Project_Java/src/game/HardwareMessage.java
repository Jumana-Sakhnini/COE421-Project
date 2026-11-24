package game;

/** One parsed line from the LilyPad, e.g. "BUTTON_PRESS,3" or "JOYSTICK,LEFT". */
public class HardwareMessage {
    private final MessageType type;
    private final int targetId;        // SHOT / BUTTON_PRESS: 1-3, otherwise 0
    private final Position position;   // JOYSTICK only, otherwise null
    private final String payload;      // the original text

    private HardwareMessage(MessageType type, int targetId, Position position, String payload) {
        this.type = type; this.targetId = targetId; this.position = position; this.payload = payload;
    }

    /** Turns a text line into a message. Anything not understood becomes UNKNOWN (ignored). */
    public static HardwareMessage parse(String line) {
        String text = line == null ? "" : line.trim();
        String[] p = text.toUpperCase().split(",");
        try {
            MessageType type = MessageType.valueOf(p[0].trim());
            switch (type) {
                case SHOT: case BUTTON_PRESS:
                    int id = Integer.parseInt(p[1].trim());
                    if (id < 1 || id > 3) break;
                    return new HardwareMessage(type, id, null, text);
                case JOYSTICK:
                    return new HardwareMessage(type, 0, Position.valueOf(p[1].trim()), text);
                case UNKNOWN:
                    break;
                default:
                    return new HardwareMessage(type, 0, null, text);
            }
        } catch (RuntimeException ignored) { /* falls through to UNKNOWN */ }
        return new HardwareMessage(MessageType.UNKNOWN, 0, null, text);
    }

    /** True for the inputs a player makes on purpose to answer a challenge. */
    public boolean isAction() {
        return type == MessageType.SHOT || type == MessageType.BUTTON_PRESS || type == MessageType.JOYSTICK;
    }

    public MessageType getType() { return type; }
    public int getTargetId() { return targetId; }
    public Position getPosition() { return position; }
    public String getPayload() { return payload; }

    /** Short description for the security log. */
    public String describe() {
        switch (type) {
            case SHOT: return "shot on the target";
            case BUTTON_PRESS: return "button " + targetId;
            case JOYSTICK: return "joystick " + position;
            default: return payload;
        }
    }

    @Override public String toString() { return payload; }
}
