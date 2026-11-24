package game.gui;

import game.Color;
import game.GameView;
import game.ZoneMode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.function.Supplier;

/** Small rotating radar with one blip per zone. */
class RadarView extends JComponent {
    private final Supplier<GameView> source;

    RadarView(Supplier<GameView> source) {
        this.source = source;
        setPreferredSize(new Dimension(320, 190));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        GameView v = source.get();
        if (v == null) return;
        long now = System.currentTimeMillis();
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        Theme.frame(g, 0, 0, w, h, "PERIMETER RADAR");
        double r = Math.min(w * 0.36, (h - 44) / 2.0), cx = w * 0.42, cy = 30 + (h - 30) / 2.0;
        g.setColor(new java.awt.Color(0x07161A));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(Theme.alpha(Theme.OK, 55));
        g.setStroke(new BasicStroke(1f));
        for (int i = 1; i <= 3; i++) {
            double rr = r * i / 3;
            g.draw(new Ellipse2D.Double(cx - rr, cy - rr, rr * 2, rr * 2));
        }
        g.draw(new Line2D.Double(cx - r, cy, cx + r, cy));
        g.draw(new Line2D.Double(cx, cy - r, cx, cy + r));
        // sweep
        double speed = "ON DUTY".equals(v.stateName) ? 1100.0 : 2600.0;
        double ang = (now % (long) speed) / speed * 360.0;
        for (int i = 0; i < 18; i++) {
            g.setColor(Theme.alpha(Theme.OK, 70 - i * 4));
            g.fill(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, -ang + i * 2.4, 2.6, Arc2D.PIE));
        }
        // zone blips in the same left-to-right order as the monitors
        double[][] pos = {{-0.62, -0.25}, {0.0, -0.62}, {0.62, -0.25}};
        for (int led = 1; led <= 3; led++) {
            int p = Theme.screenPos(led);
            double bx = cx + pos[p][0] * r, by = cy + pos[p][1] * r;
            Color lit = ZoneView.ledColour(v, led, now);
            boolean threat = v.mode == ZoneMode.RESPOND && v.challengeLed == led && v.challengeColor != Color.GREEN;
            java.awt.Color c = lit != Color.OFF ? Theme.led(lit) : Theme.alpha(Theme.OK, 120);
            if (threat) Theme.glow(g, bx, by, 16 + 4 * Math.sin(now / 120.0), c, 200);
            g.setColor(c);
            g.fill(new Ellipse2D.Double(bx - 4, by - 4, 8, 8));
            g.setFont(Theme.mono(Font.BOLD, 9.5f));
            g.setColor(Theme.alpha(Theme.TEXT, 170));
            g.drawString(String.valueOf((char) ('A' + led - 1)), (float) bx + 6, (float) by - 5);
        }
        // legend
        int lx = (int) (cx + r + 18), ly = 50;
        g.setFont(Theme.mono(Font.PLAIN, 10f));
        String[][] legend = {{"RED", "SHOOT"}, {"BLUE", "BUTTON"}, {"GREEN", "CODE"}, {"CLAP", "SLOW"}};
        java.awt.Color[] lc = {Theme.LED_RED, Theme.LED_BLUE, Theme.LED_GREEN, Theme.CYAN};
        for (int i = 0; i < legend.length; i++) {
            int y = ly + i * 30;
            g.setColor(lc[i]);
            g.fillOval(lx, y - 8, 9, 9);
            g.setColor(Theme.TEXT);
            g.drawString(legend[i][0], lx + 15, y);
            g.setColor(Theme.DIM);
            g.drawString(legend[i][1], lx + 15, y + 12);
        }
        g.dispose();
    }
}
