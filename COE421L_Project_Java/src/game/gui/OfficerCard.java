package game.gui;

import game.GameView;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.function.Supplier;

/** Officer on duty: name, hearts, score, streak, level and progress. */
class OfficerCard extends JComponent {
    private final Supplier<GameView> source;

    OfficerCard(Supplier<GameView> source) {
        this.source = source;
        setPreferredSize(new Dimension(320, 300));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        GameView v = source.get();
        if (v == null) return;
        long now = System.currentTimeMillis();
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        Theme.frame(g, 0, 0, w, h, "OFFICER ON DUTY");

        // badge + name
        Shape badge = Theme.shield(16, 36, 34, 40);
        g.setColor(Theme.alpha(Theme.AMBER, 50));
        g.fill(badge);
        g.setColor(Theme.AMBER);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(badge);
        g.setFont(Theme.mono(Font.BOLD, 14f));
        Theme.centered(g, String.valueOf(Math.max(1, v.officerNumber)), 33, 62);
        g.setFont(Theme.sans(Font.BOLD, 20f));
        g.setColor(Theme.TEXT);
        String name = v.officerName.isEmpty() ? "\u2014" : v.officerName.toUpperCase();
        g.drawString(fit(g, name, w - 76), 60, 56);
        g.setFont(Theme.mono(Font.PLAIN, 11f));
        g.setColor(Theme.DIM);
        g.drawString(v.officerCount == 0 ? "NO TEAM" : "OFFICER " + v.officerNumber + " OF " + v.officerCount, 60, 73);

        // hearts
        long sinceWrong = now - v.lastWrong;
        for (int i = 0; i < v.maxHearts; i++) {
            double s = 34, x = 16 + i * (s + 10), y = 90;
            boolean full = i < v.hearts;
            boolean justLost = !full && i == v.hearts && v.lastWrong > 0 && sinceWrong < 700;
            if (justLost) x += Math.sin(sinceWrong / 25.0) * 4 * (1 - sinceWrong / 700.0);
            Shape ht = Theme.heart(x, y, s);
            if (full) {
                Theme.glow(g, x + s / 2, y + s / 2, s * 0.9, Theme.HEART, 70);
                g.setPaint(new GradientPaint((float) x, (float) y, new java.awt.Color(0xFF6B85), (float) x, (float) (y + s), Theme.HEART));
                g.fill(ht);
            } else {
                g.setColor(justLost ? Theme.alpha(Theme.HEART, (int) (200 * (1 - sinceWrong / 700.0))) : Theme.PANEL_2);
                g.fill(ht);
                g.setColor(Theme.FAINT);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(ht);
            }
        }
        g.setFont(Theme.mono(Font.BOLD, 11f));
        g.setColor(v.hearts <= 1 && "ON DUTY".equals(v.stateName) ? Theme.ALERT : Theme.DIM);
        g.drawString(v.hearts + "/" + v.maxHearts + " LIVES", 152, 112);

        // score
        g.setFont(Theme.mono(Font.PLAIN, 10.5f));
        g.setColor(Theme.DIM);
        g.drawString("SCORE", 16, 148);
        g.setFont(Theme.mono(Font.BOLD, 38f));
        g.setColor(Theme.TEXT);
        g.drawString(String.format("%05d", v.score), 14, 186);
        // streak / combo
        int bx = w - 112;
        g.setFont(Theme.mono(Font.PLAIN, 10.5f));
        g.setColor(Theme.DIM);
        g.drawString("STREAK", bx, 148);
        g.setFont(Theme.mono(Font.BOLD, 22f));
        g.setColor(v.combo ? Theme.AMBER : Theme.TEXT);
        g.drawString("x" + v.streak, bx, 174);
        if (v.combo) {
            g.setFont(Theme.mono(Font.BOLD, 10.5f));
            int a = (int) (170 + 85 * Math.sin(now / 140.0));
            g.setColor(Theme.alpha(Theme.AMBER, a));
            g.fillRoundRect(bx, 180, 62, 16, 16, 16);
            g.setColor(Theme.BG);
            g.drawString("COMBO", bx + 13, 192);
        }

        // level segments
        g.setFont(Theme.mono(Font.PLAIN, 10.5f));
        g.setColor(Theme.DIM);
        g.drawString("LEVEL  \u00B7  " + v.levelName, 16, 218);
        int segW = (w - 32 - 16) / 3;
        for (int i = 1; i <= 3; i++) {
            int sx = 16 + (i - 1) * (segW + 8);
            boolean cur = i == v.level, past = i < v.level;
            java.awt.Color c = i == 1 ? Theme.OK : i == 2 ? Theme.AMBER : Theme.ALERT;
            g.setColor(cur ? Theme.alpha(c, 200) : past ? Theme.alpha(c, 70) : Theme.PANEL_2);
            g.fillRoundRect(sx, 226, segW, 20, 6, 6);
            g.setColor(cur ? c : Theme.EDGE);
            g.drawRoundRect(sx, 226, segW, 20, 6, 6);
            g.setFont(Theme.mono(Font.BOLD, 11f));
            g.setColor(cur ? Theme.BG : past ? Theme.TEXT : Theme.DIM);
            Theme.centered(g, i == 1 ? "1 SLOW" : i == 2 ? "2 FASTER" : "3 \u221E", sx + segW / 2.0, 240);
        }

        // progress
        int py = 268;
        if (v.levelGoal > 0) {
            g.setFont(Theme.mono(Font.PLAIN, 10.5f));
            g.setColor(Theme.DIM);
            g.drawString("ZONES SECURED", 16, py);
            g.setColor(Theme.TEXT);
            g.setFont(Theme.mono(Font.BOLD, 11f));
            g.drawString(v.levelCorrect + " / " + v.levelGoal, w - 58, py);
            int pw = (w - 32 - (v.levelGoal - 1) * 6) / v.levelGoal;
            for (int i = 0; i < v.levelGoal; i++) {
                g.setColor(i < v.levelCorrect ? Theme.OK : Theme.PANEL_2);
                g.fillRoundRect(16 + i * (pw + 6), py + 8, pw, 10, 10, 10);
            }
        } else {
            g.setFont(Theme.mono(Font.PLAIN, 10.5f));
            g.setColor(Theme.DIM);
            g.drawString("LOCKDOWN SPEED", 16, py);
            // 2600 ms -> 0 %, 900 ms -> 100 %
            double speed = Math.max(0, Math.min(1, (2600 - (v.windowMs - v.slowMs)) / 1700.0));
            if (v.windowMs == 0) speed = 0;
            g.setColor(Theme.TEXT);
            g.setFont(Theme.mono(Font.BOLD, 11f));
            String t = "+" + v.levelCorrect + " CLEARED";
            g.drawString(t, w - 16 - g.getFontMetrics().stringWidth(t), py);
            g.setColor(Theme.PANEL_2);
            g.fillRoundRect(16, py + 8, w - 32, 10, 10, 10);
            g.setPaint(new GradientPaint(16, 0, Theme.AMBER, w - 16, 0, Theme.ALERT));
            g.fillRoundRect(16, py + 8, (int) Math.max(10, (w - 32) * speed), 10, 10, 10);
        }
        g.dispose();
    }

    static String fit(Graphics2D g, String s, int max) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= max) return s;
        while (s.length() > 1 && fm.stringWidth(s + "\u2026") > max) s = s.substring(0, s.length() - 1);
        return s + "\u2026";
    }
}
