package game.gui;

import game.GameView;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.function.Supplier;

/** Top bar: shield emblem, title, threat level, link status and clock. */
class HeaderBar extends JComponent {
    private final Supplier<GameView> source;
    private final String portName;

    HeaderBar(Supplier<GameView> source, String portName) {
        this.source = source;
        this.portName = portName;
        setPreferredSize(new Dimension(1200, 66));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        GameView v = source.get();
        if (v == null) return;
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        long now = System.currentTimeMillis();
        g.setPaint(new GradientPaint(0, 0, new java.awt.Color(0x0E1823), 0, h, new java.awt.Color(0x080D13)));
        g.fillRect(0, 0, w, h);
        g.setColor(Theme.EDGE);
        g.drawLine(0, h - 1, w, h - 1);
        // hazard strip
        g.setColor(Theme.alpha(Theme.AMBER, 50));
        for (int x = -20; x < w; x += 22) {
            Path2D st = new Path2D.Double();
            st.moveTo(x, h); st.lineTo(x + 8, h); st.lineTo(x + 12, h - 4); st.lineTo(x + 4, h - 4); st.closePath();
            g.fill(st);
        }

        // emblem
        Shape sh = Theme.shield(16, 9, 38, 46);
        g.setPaint(new GradientPaint(16, 9, new java.awt.Color(0x1C6E7A), 16, 55, new java.awt.Color(0x0A2B33)));
        g.fill(sh);
        g.setColor(Theme.CYAN);
        g.setStroke(new BasicStroke(2f));
        g.draw(sh);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Ellipse2D.Double(27, 22, 16, 16));
        g.draw(new Line2D.Double(35, 17, 35, 43));
        g.draw(new Line2D.Double(22, 30, 48, 30));

        g.setFont(Theme.sans(Font.BOLD, 21f));
        g.setColor(Theme.TEXT);
        g.drawString("SENTRY TURRET", 68, 31);
        int tw = g.getFontMetrics().stringWidth("SENTRY TURRET");
        g.setColor(Theme.CYAN);
        g.drawString("  \u2014  SECURITY CONTROL", 68 + tw, 31);
        g.setFont(Theme.mono(Font.PLAIN, 11f));
        g.setColor(Theme.DIM);
        g.drawString("COE421L  \u00B7  PERIMETER DEFENSE SYSTEM  \u00B7  3 ZONES ONLINE", 69, 49);

        // right side pills
        int x = w - 16;
        g.setFont(Theme.mono(Font.BOLD, 20f));
        String clock = java.time.LocalTime.now().withNano(0).toString();
        int cw = g.getFontMetrics().stringWidth(clock);
        x -= cw;
        g.setColor(Theme.TEXT);
        g.drawString(clock, x, 40);
        x -= 18;
        x = pill(g, x, v.connected ? "XBEE LINK \u00B7 " + portName : "SIMULATION MODE",
                v.connected ? Theme.OK : Theme.AMBER, true, now);
        x -= 10;
        String threat = "THREAT LEVEL " + v.level;
        java.awt.Color tc = v.level == 1 ? Theme.OK : v.level == 2 ? Theme.AMBER : Theme.ALERT;
        x = pill(g, x, threat, tc, v.level == 3 && "ON DUTY".equals(v.stateName), now);
        x -= 10;
        pill(g, x, v.stateName, Theme.CYAN, false, now);
        g.dispose();
    }

    /** Draws a status pill ending at x (right aligned); returns its left edge. */
    private int pill(Graphics2D g, int right, String text, java.awt.Color c, boolean pulse, long now) {
        g.setFont(Theme.mono(Font.BOLD, 11.5f));
        int tw = g.getFontMetrics().stringWidth(text);
        int pw = tw + 34, x = right - pw, y = 19, ph = 26;
        g.setColor(Theme.alpha(c, 30));
        g.fillRoundRect(x, y, pw, ph, ph, ph);
        g.setColor(Theme.alpha(c, 150));
        g.drawRoundRect(x, y, pw, ph, ph, ph);
        int a = pulse ? (int) (140 + 115 * Math.sin(now / 160.0)) : 255;
        g.setColor(Theme.alpha(c, a));
        g.fillOval(x + 11, y + 9, 8, 8);
        g.setColor(Theme.mix(c, java.awt.Color.WHITE, 0.35));
        g.drawString(text, x + 25, y + 17);
        return x;
    }
}
