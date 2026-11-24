package game.gui;

import game.GameView;
import game.ZoneMode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.function.Supplier;

/**
 * Full-size layer over the three monitors: "START SHIFT", level banners,
 * "OFFICER DOWN" and the leaderboard. Transparent when the game is running.
 */
class Overlay extends JComponent {
    private final Supplier<GameView> source;
    private final ConsoleButton newShift = new ConsoleButton("NEW SHIFT  (same team)", Theme.OK);
    private final ConsoleButton newTeam = new ConsoleButton("NEW TEAM", Theme.AMBER);
    private final ConsoleButton begin = new ConsoleButton("\u25B6  START SHIFT", Theme.AMBER);

    Overlay(Supplier<GameView> source, Runnable onStartShift, Runnable onNewTeam) {
        this.source = source;
        setLayout(null);
        newShift.addActionListener(e -> onStartShift.run());
        newTeam.addActionListener(e -> onNewTeam.run());
        begin.addActionListener(e -> onStartShift.run());
        add(newShift);
        add(newTeam);
        add(begin);
        newShift.setVisible(false);
        newTeam.setVisible(false);
        begin.setVisible(false);
    }

    /** Called every frame: shows the buttons that belong to the current screen. */
    void sync() {
        GameView v = source.get();
        if (v == null) return;
        boolean board = "LEADERBOARD".equals(v.stateName);
        boolean waiting = "WAITING FOR OFFICER".equals(v.stateName);
        if (newShift.isVisible() != board) { newShift.setVisible(board); newTeam.setVisible(board); }
        if (begin.isVisible() != waiting) begin.setVisible(waiting);
        int w = getWidth(), h = getHeight();
        Dimension a = newShift.getPreferredSize(), b = newTeam.getPreferredSize(), c = begin.getPreferredSize();
        newShift.setBounds(w / 2 - a.width - 8, h - 70, a.width, a.height);
        newTeam.setBounds(w / 2 + 8, h - 70, b.width, b.height);
        begin.setBounds(w / 2 - c.width / 2, (int) (h * 0.5 + 150), c.width, c.height);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        GameView v = source.get();
        if (v == null) return;
        long now = System.currentTimeMillis();
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        if ("SETUP".equals(v.stateName)) {
            dim(g, w, h, 200);
            title(g, w, h * 0.5, "AWAITING SECURITY TEAM", Theme.CYAN, 30f);
        } else if ("WAITING FOR OFFICER".equals(v.stateName)) {
            dim(g, w, h, 185);
            drawCallOfficer(g, v, w, h, now);
        } else if ("LEADERBOARD".equals(v.stateName)) {
            dim(g, w, h, 225);
            drawLeaderboard(g, v, w, h);
        } else if (v.mode == ZoneMode.BANNER) {
            drawBanner(g, v, w, h, now);
        } else if (v.mode == ZoneMode.OFFICER_DOWN) {
            drawDown(g, v, w, h, now);
        }
        g.dispose();
    }

    private void dim(Graphics2D g, int w, int h, int a) {
        g.setColor(Theme.alpha(Theme.BG, a));
        g.fillRoundRect(0, 0, w, h, 12, 12);
    }

    private void title(Graphics2D g, int w, double y, String s, java.awt.Color c, float size) {
        g.setFont(Theme.sans(Font.BOLD, size));
        g.setColor(c);
        Theme.centered(g, s, w / 2.0, y);
    }

    private void drawCallOfficer(Graphics2D g, GameView v, int w, int h, long now) {
        double cy = h * 0.5;
        Shape sh = Theme.shield(w / 2.0 - 34, cy - 190, 68, 80);
        g.setColor(Theme.alpha(Theme.AMBER, 45));
        g.fill(sh);
        g.setColor(Theme.AMBER);
        g.setStroke(new BasicStroke(2.2f));
        g.draw(sh);
        g.setFont(Theme.mono(Font.BOLD, 28f));
        Theme.centered(g, String.valueOf(v.officerNumber), w / 2.0, cy - 135);
        g.setFont(Theme.mono(Font.BOLD, 14f));
        g.setColor(Theme.DIM);
        Theme.centered(g, "OFFICER " + v.officerNumber + " OF " + v.officerCount + " REPORTING FOR DUTY", w / 2.0, cy - 80);
        title(g, w, cy - 34, v.officerName.toUpperCase(), Theme.TEXT, 42f);
        int a = (int) (150 + 105 * Math.sin(now / 220.0));
        g.setFont(Theme.mono(Font.BOLD, 16f));
        g.setColor(Theme.alpha(Theme.AMBER, a));
        Theme.centered(g, "PRESS  START SHIFT  WHEN YOU ARE READY", w / 2.0, cy + 8);

        // briefing
        String[][] rules = {
                {"RED", "Intruder \u2014 shoot the middle target"},
                {"BLUE", "Breach \u2014 press the button under the light"},
                {"GREEN", "Access code \u2014 repeat it with the joystick"},
                {"CLAP", "Slow the system down"},
        };
        java.awt.Color[] cs = {Theme.LED_RED, Theme.LED_BLUE, Theme.LED_GREEN, Theme.CYAN};
        double bw = Math.min(680, w - 40), bx = w / 2.0 - bw / 2, by = cy + 30;
        g.setColor(Theme.alpha(Theme.PANEL, 230));
        g.fill(new RoundRectangle2D.Double(bx, by, bw, 108, 10, 10));
        g.setColor(Theme.EDGE);
        g.draw(new RoundRectangle2D.Double(bx, by, bw, 108, 10, 10));
        g.setFont(Theme.mono(Font.PLAIN, 12f));
        for (int i = 0; i < rules.length; i++) {
            double ry = by + 22 + i * 20;
            g.setColor(cs[i]);
            g.fill(new Ellipse2D.Double(bx + 18, ry - 9, 10, 10));
            g.setFont(Theme.mono(Font.BOLD, 12f));
            g.drawString(rules[i][0], (float) bx + 36, (float) ry);
            g.setFont(Theme.mono(Font.PLAIN, 12f));
            g.setColor(Theme.TEXT);
            g.drawString(rules[i][1], (float) bx + 96, (float) ry);
        }
        g.setColor(Theme.HEART);
        for (int i = 0; i < 3; i++) g.fill(Theme.heart(bx + bw - 108 + i * 30, by + 18, 24));
        g.setFont(Theme.mono(Font.PLAIN, 10.5f));
        g.setColor(Theme.DIM);
        g.drawString("3 LIVES PER SHIFT", (float) (bx + bw - 140), (float) by + 62);
        g.drawString("L1 + L2: SECURE 5", (float) (bx + bw - 140), (float) by + 78);
        g.drawString("L3: SURVIVE", (float) (bx + bw - 140), (float) by + 94);
    }

    private void drawBanner(Graphics2D g, GameView v, int w, int h, long now) {
        double t = Math.min(1, (now - v.modeStart) / 350.0);
        double bh = 150 * t, y = h / 2.0 - bh / 2;
        java.awt.Color c = v.level == 1 ? Theme.OK : v.level == 2 ? Theme.AMBER : Theme.ALERT;
        g.setColor(Theme.alpha(Theme.BG, 215));
        g.fill(new Rectangle2D.Double(0, y, w, bh));
        Shape old = g.getClip();
        g.clip(new Rectangle2D.Double(0, y, w, 8));
        stripes(g, w, y, 8, c, now);
        g.setClip(old);
        g.clip(new Rectangle2D.Double(0, y + bh - 8, w, 8));
        stripes(g, w, y + bh - 8, 8, c, now);
        g.setClip(old);
        if (t < 1) return;
        String[] parts = v.banner.split(" - ", 2);
        title(g, w, h / 2.0 + 4, parts[0], c, 54f);
        g.setFont(Theme.mono(Font.BOLD, 15f));
        g.setColor(Theme.TEXT);
        String sub = (parts.length > 1 ? parts[1] : "") + "   \u00B7   "
                + (v.level == 1 ? "SLOW \u00B7 SECURE 5 ZONES" : v.level == 2 ? "FASTER \u00B7 JOYSTICK CODES \u00B7 SECURE 5 ZONES"
                : "ENDLESS \u00B7 FASTER WITH EVERY HIT \u00B7 WATCH FOR DECOYS");
        Theme.centered(g, sub, w / 2.0, h / 2.0 + 40);
    }

    private void stripes(Graphics2D g, int w, double y, double bh, java.awt.Color c, long now) {
        g.setColor(c);
        g.fill(new Rectangle2D.Double(0, y, w, bh));
        g.setColor(Theme.BG);
        double off = (now / 20.0) % 28;
        for (double x = -28 + off; x < w + 28; x += 28) {
            Path2D st = new Path2D.Double();
            st.moveTo(x, y + bh); st.lineTo(x + 12, y + bh); st.lineTo(x + 12 + bh, y); st.lineTo(x + bh, y); st.closePath();
            g.fill(st);
        }
    }

    private void drawDown(Graphics2D g, GameView v, int w, int h, long now) {
        g.setColor(Theme.alpha(new java.awt.Color(0x2A0508), 170));
        g.fillRoundRect(0, 0, w, h, 12, 12);
        int a = (int) (180 + 75 * Math.sin(now / 150.0));
        g.setColor(Theme.alpha(java.awt.Color.BLACK, 190));
        g.fill(new Rectangle2D.Double(0, h * 0.45 - 70, w, 160));
        title(g, w, h * 0.45, "OFFICER DOWN", Theme.alpha(Theme.ALERT, a), 60f);
        String result = v.sessionResults.stream().filter(r -> r[0].equals(v.officerName)).findFirst()
                .map(r -> "FINAL SCORE " + r[1] + "   \u00B7   REACHED LEVEL " + r[2]).orElse("");
        g.setFont(Theme.mono(Font.BOLD, 16f));
        g.setColor(Theme.TEXT);
        Theme.centered(g, v.officerName.toUpperCase() + "   \u00B7   " + result, w / 2.0, h * 0.45 + 46);
        g.setFont(Theme.mono(Font.PLAIN, 12f));
        g.setColor(Theme.DIM);
        Theme.centered(g, v.officerNumber < v.officerCount ? "next officer stand by ..." : "compiling shift report ...",
                w / 2.0, h * 0.45 + 74);
    }

    private void drawLeaderboard(Graphics2D g, GameView v, int w, int h) {
        title(g, w, 58, "SHIFT REPORT  \u2014  LEADERBOARD", Theme.CYAN, 30f);
        g.setFont(Theme.mono(Font.PLAIN, 12f));
        g.setColor(Theme.DIM);
        Theme.centered(g, "all officers have completed their shift  \u00B7  NEW SHIFT plays again with the same team", w / 2.0, 82);

        double tw = Math.min(560, w * 0.58), tx = Math.max(20, w * 0.04), ty = 108;
        double aw = w - tx * 2 - tw - 24, ax = tx + tw + 24;
        // session table
        panel(g, tx, ty, tw, h - ty - 90, "THIS SESSION");
        String[] head = {"RANK", "OFFICER", "SCORE", "LEVEL"};
        double[] col = {tx + 18, tx + 90, tx + tw - 170, tx + tw - 74};
        g.setFont(Theme.mono(Font.BOLD, 11f));
        g.setColor(Theme.DIM);
        for (int i = 0; i < head.length; i++) g.drawString(head[i], (float) col[i], (float) ty + 50);
        java.awt.Color[] medal = {new java.awt.Color(0xFFD24A), new java.awt.Color(0xC9D3DC), new java.awt.Color(0xD08A4E)};
        int rows = (int) Math.max(1, (h - ty - 160) / 34);
        for (int i = 0; i < Math.min(rows, v.sessionResults.size()); i++) {
            String[] r = v.sessionResults.get(i);
            double y = ty + 62 + i * 34;
            g.setColor(i == 0 ? Theme.alpha(medal[0], 30) : i % 2 == 0 ? Theme.alpha(Theme.PANEL_2, 200) : Theme.alpha(Theme.PANEL, 0));
            g.fill(new RoundRectangle2D.Double(tx + 8, y, tw - 16, 30, 6, 6));
            java.awt.Color mc = i < 3 ? medal[i] : Theme.DIM;
            g.setColor(mc);
            g.fill(new Ellipse2D.Double(col[0], y + 6, 18, 18));
            g.setColor(Theme.BG);
            g.setFont(Theme.mono(Font.BOLD, 11f));
            Theme.centered(g, String.valueOf(i + 1), col[0] + 9, y + 19);
            g.setFont(Theme.sans(Font.BOLD, 15f));
            g.setColor(i == 0 ? medal[0] : Theme.TEXT);
            g.drawString(OfficerCard.fit(g, r[0], (int) (col[2] - col[1] - 12)) + (i == 0 ? "  \u2605" : ""), (float) col[1], (float) y + 21);
            g.setFont(Theme.mono(Font.BOLD, 15f));
            g.setColor(Theme.TEXT);
            g.drawString(r[1], (float) col[2], (float) y + 21);
            g.setColor(Integer.parseInt(r[2]) >= 3 ? Theme.ALERT : Integer.parseInt(r[2]) == 2 ? Theme.AMBER : Theme.OK);
            g.drawString("L" + r[2], (float) col[3], (float) y + 21);
        }
        // all-time
        if (aw > 160) {
            panel(g, ax, ty, aw, h - ty - 90, "ALL-TIME TOP 5");
            for (int i = 0; i < v.allTimeTop.size(); i++) {
                String[] r = v.allTimeTop.get(i);
                double y = ty + 58 + i * 30;
                g.setFont(Theme.mono(Font.BOLD, 12f));
                g.setColor(i < 3 ? medal[i] : Theme.DIM);
                g.drawString((i + 1) + ".", (float) ax + 16, (float) y);
                g.setColor(Theme.TEXT);
                g.drawString(OfficerCard.fit(g, r[0], (int) aw - 110), (float) ax + 40, (float) y);
                String s = r[1].trim();
                g.drawString(s, (float) (ax + aw - 16 - g.getFontMetrics().stringWidth(s)), (float) y);
            }
        }
    }

    private void panel(Graphics2D g, double x, double y, double w, double h, String title) {
        g.setColor(Theme.alpha(Theme.PANEL, 240));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
        g.setColor(Theme.EDGE);
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
        g.setFont(Theme.mono(Font.BOLD, 11f));
        g.setColor(Theme.CYAN);
        g.drawString(title, (float) x + 16, (float) y + 24);
    }
}
