package game.gui;

import game.GameView;
import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/** Security log: newest event on top, coloured by type. */
class LogView extends JComponent {
    private final Supplier<GameView> source;

    LogView(Supplier<GameView> source) {
        this.source = source;
        setPreferredSize(new Dimension(320, 220));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        GameView v = source.get();
        if (v == null) return;
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        Theme.frame(g, 0, 0, w, h, "SECURITY LOG");
        g.setFont(Theme.mono(Font.PLAIN, 10.5f));
        FontMetrics fm = g.getFontMetrics();
        int y = 44, lh = fm.getHeight() + 1;
        g.clipRect(8, 30, w - 16, h - 38);
        for (String line : v.log) {
            if (y > h - 8) break;
            g.setColor(colour(line));
            g.drawString(OfficerCard.fit(g, line, w - 28), 14, y);
            y += lh;
        }
        g.dispose();
    }

    private static java.awt.Color colour(String s) {
        if (s.contains("SECURED") || s.contains("accepted") || s.contains("SHIFT STARTED")) return Theme.OK;
        if (s.contains("WRONG") || s.contains("DOWN") || s.contains("TOO SLOW") || s.contains("DECOY")
                || s.contains("heart")) return Theme.ALERT;
        if (s.contains("LEVEL") || s.contains("CLAP") || s.contains("COMPLETE")) return Theme.AMBER;
        if (s.contains("INTRUDER") || s.contains("BREACH") || s.contains("CODE")) return Theme.CYAN;
        if (s.contains("ignored")) return Theme.FAINT;
        return Theme.DIM;
    }
}
