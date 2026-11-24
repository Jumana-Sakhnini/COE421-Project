package game.gui;

import game.Color;
import game.GameView;
import game.Position;
import game.ZoneMode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.function.Supplier;

/** Strip under the monitors: the order to the officer, the access code, and the response timer. */
class ChallengeBar extends JComponent {
    private final Supplier<GameView> source;

    ChallengeBar(Supplier<GameView> source) {
        this.source = source;
        setPreferredSize(new Dimension(900, 112));
    }

    static String order(GameView v) {
        switch (v.mode) {
            case BANNER:   return "STAND BY  \u2014  LEVEL " + v.level;
            case SCANNING: return "SWEEPING PERIMETER ...";
            case DECOY:    return "UNCONFIRMED SIGNAL  \u2014  HOLD FIRE!";
            case CODE_SHOW:return "ACCESS CODE  \u2014  MEMORISE THE GREEN LIGHTS";
            case RESPOND:
                if (v.challengeColor == Color.RED)  return "INTRUDER IN ZONE B  \u2014  SHOOT THE MIDDLE TARGET";
                if (v.challengeColor == Color.BLUE) return "BREACH IN ZONE " + (char) ('A' + v.challengeLed - 1)
                        + "  \u2014  PRESS BUTTON " + v.challengeLed;
                return "ENTER THE ACCESS CODE WITH THE JOYSTICK";
            case SECURED:  return "ZONE SECURED  +" + v.lastPoints + (v.combo ? "   COMBO x" + v.streak : "");
            case FAILED:   return "TOO SLOW  \u2014  ZONE BREACHED  (-1 \u2665)";
            case OFFICER_DOWN: return "OFFICER DOWN";
            default:
                if ("WAITING FOR OFFICER".equals(v.stateName)) return "AWAITING OFFICER  \u2014  PRESS START SHIFT";
                if ("LEADERBOARD".equals(v.stateName)) return "ALL SHIFTS COMPLETE";
                return "SYSTEM IDLE";
        }
    }

    static java.awt.Color orderColour(GameView v) {
        switch (v.mode) {
            case RESPOND: return Theme.led(v.challengeColor);
            case CODE_SHOW: return Theme.LED_GREEN;
            case DECOY: return Theme.AMBER;
            case SECURED: return Theme.OK;
            case FAILED: case OFFICER_DOWN: return Theme.ALERT;
            default: return Theme.CYAN;
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        GameView v = source.get();
        if (v == null) return;
        long now = System.currentTimeMillis();
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        Theme.frame(g, 0, 0, w, h, "THREAT RESPONSE");

        // order text
        java.awt.Color oc = orderColour(v);
        g.setFont(Theme.sans(Font.BOLD, 19f));
        boolean urgent = v.mode == ZoneMode.RESPOND || v.mode == ZoneMode.DECOY;
        g.setColor(urgent && (now / 350) % 2 == 1 ? Theme.mix(oc, java.awt.Color.WHITE, 0.4) : oc);
        g.drawString(order(v), 18, 50);

        // access code boxes
        int codeX = 18, codeY = 66, box = 34;
        g.setFont(Theme.mono(Font.BOLD, 10.5f));
        g.setColor(Theme.DIM);
        g.drawString("ACCESS CODE", codeX, codeY + 22);
        codeX += 92;
        int n = v.pattern.isEmpty() ? 4 : v.pattern.size();
        for (int i = 0; i < n; i++) {
            double bx = codeX + i * (box + 8);
            RoundRectangle2D r = new RoundRectangle2D.Double(bx, codeY, box, box, 8, 8);
            boolean has = !v.pattern.isEmpty();
            boolean shown = has && v.mode == ZoneMode.CODE_SHOW && i < v.patternRevealed;
            boolean done = has && v.mode == ZoneMode.RESPOND && i < v.patternDone;
            boolean next = has && v.mode == ZoneMode.RESPOND && i == v.patternDone;
            g.setColor(done ? Theme.alpha(Theme.LED_GREEN, 70) : Theme.PANEL_2);
            g.fill(r);
            g.setColor(next ? Theme.alpha(Theme.LED_GREEN, (now / 250) % 2 == 0 ? 255 : 90)
                    : has ? Theme.alpha(Theme.LED_GREEN, 110) : Theme.FAINT);
            g.setStroke(new BasicStroke(next ? 2f : 1.2f));
            g.draw(r);
            if (has && (shown || done)) {
                g.setColor(Theme.LED_GREEN);
                g.fill(Theme.arrow(v.pattern.get(i), bx + box / 2.0, codeY + box / 2.0, box * 0.62));
            } else if (has) {
                g.setColor(Theme.alpha(Theme.LED_GREEN, 120));
                g.setFont(Theme.mono(Font.BOLD, 16f));
                Theme.centered(g, "?", bx + box / 2.0, codeY + box / 2.0 + 6);
            }
        }

        // response timer
        int tx = Math.max(codeX + 4 * (box + 8) + 30, w / 2 + 40), tw = w - tx - 20, ty = 80, th = 18;
        g.setFont(Theme.mono(Font.BOLD, 10.5f));
        g.setColor(Theme.DIM);
        g.drawString("RESPONSE TIME", tx, ty - 6);
        double frac = 0;
        long left = 0;
        if (v.deadline > 0 && v.windowMs > 0) {
            left = Math.max(0, v.deadline - now);
            frac = Math.min(1, left / (double) v.windowMs);
        }
        g.setColor(Theme.PANEL_2);
        g.fillRoundRect(tx, ty, tw, th, th, th);
        java.awt.Color bar = frac > 0.5 ? Theme.OK : frac > 0.25 ? Theme.AMBER : Theme.ALERT;
        if (frac > 0) {
            g.setColor(bar);
            g.fillRoundRect(tx, ty, (int) Math.max(th, tw * frac), th, th, th);
        }
        g.setColor(Theme.EDGE);
        g.drawRoundRect(tx, ty, tw, th, th, th);
        g.setFont(Theme.mono(Font.BOLD, 12f));
        g.setColor(Theme.TEXT);
        String secs = v.deadline > 0 ? String.format("%.1f s", left / 1000.0) : "--.- s";
        g.drawString(secs, tx + tw - g.getFontMetrics().stringWidth(secs), ty - 6);
        if (v.slowMs > 0) {
            boolean fresh = now - v.lastClap < 1500;
            String tag = "SLOWED +" + (v.slowMs / 100) / 10.0 + " s";
            g.setFont(Theme.mono(Font.BOLD, 10.5f));
            int sw = g.getFontMetrics().stringWidth(tag) + 16;
            int sx = tx + tw - sw - 70;
            g.setColor(Theme.alpha(Theme.CYAN, fresh ? 120 : 45));
            g.fillRoundRect(sx, ty - 19, sw, 16, 16, 16);
            g.setColor(Theme.CYAN);
            g.drawString(tag, sx + 8, ty - 7);
        }
        g.dispose();
    }

    static String label(Position p) { return p == Position.CENTER ? "UP/DOWN" : p.name(); }
}
