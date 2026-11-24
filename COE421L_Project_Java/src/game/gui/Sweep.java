package game.gui;

import game.Color;
import game.GameView;

/**
 * Mirrors the LilyPad's sweep so the screen and the real LEDs move together:
 * step s lights LED (3 - s % 3) -> moving to the LEFT, coloured RED, GREEN, BLUE in turn.
 */
final class Sweep {
    private Sweep() { }
    private static final Color[] COLOURS = {Color.RED, Color.GREEN, Color.BLUE};

    static int step(GameView v, long now) {
        if (v.scanStepMs <= 0) return 0;
        return (int) (Math.max(0, now - v.modeStart) / v.scanStepMs);
    }

    /** LED 1-3 lit by the sweep right now. */
    static int led(GameView v, long now) { return 3 - (step(v, now) % 3); }

    static Color colour(GameView v, long now) { return COLOURS[step(v, now) % 3]; }

    /** 0..1 progress inside the current step (for smooth movement). */
    static double frac(GameView v, long now) {
        if (v.scanStepMs <= 0) return 0;
        return (Math.max(0, now - v.modeStart) % v.scanStepMs) / (double) v.scanStepMs;
    }
}
