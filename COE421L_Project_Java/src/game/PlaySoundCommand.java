package game;

/** "SOUND,<TYPE>,<playerID>" - play a buzzer sound (START, SUCCESS, COMBO, FAILURE, ...). */
public class PlaySoundCommand implements HardwareCommand {
    private final SoundType tone;
    private final int playerID;

    public PlaySoundCommand(SoundType tone, int playerID) { this.tone = tone; this.playerID = playerID; }

    @Override public void execute() { }
    @Override public String toProtocolMessage() { return "SOUND," + tone + "," + playerID; }
}
