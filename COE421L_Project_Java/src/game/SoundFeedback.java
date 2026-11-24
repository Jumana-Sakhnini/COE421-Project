package game;

/**
 * Observer: plays a sound when a target round ends - SUCCESS, COMBO (3+ in a row)
 * or FAILURE. It is registered after the ScoreBoard, so the streak is already updated.
 */
public class SoundFeedback implements TargetObserver {
    private final CommunicationManager commManager;
    private final ScoreBoard scoreBoard;

    public SoundFeedback(CommunicationManager commManager, ScoreBoard scoreBoard) {
        this.commManager = commManager;
        this.scoreBoard = scoreBoard;
    }

    @Override
    public void onTargetHit(Target target, boolean success) {
        playTone(selectTone(success));
    }

    public SoundType selectTone(boolean success) {
        if (!success) return SoundType.FAILURE;
        return scoreBoard.isCombo() ? SoundType.COMBO : SoundType.SUCCESS;
    }

    public void playTone(SoundType tone) {
        commManager.sendCommand(new PlaySoundCommand(tone, 0));
    }
}
