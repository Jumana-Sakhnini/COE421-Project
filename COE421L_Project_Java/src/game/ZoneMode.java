package game;

/** What the security screen is showing right now. */
public enum ZoneMode {
    IDLE,          // waiting / between challenges
    BANNER,        // "LEVEL x" announcement
    SCANNING,      // red/green/blue sweep moving to the left
    DECOY,         // fake dim flicker (level 3) - do nothing!
    CODE_SHOW,     // green access code being shown
    RESPOND,       // a challenge is up and the timer runs
    SECURED,       // correct
    FAILED,        // time-out
    OFFICER_DOWN,  // last heart lost
}
