package game.gui;

import game.Color;
import java.awt.*;
import java.awt.geom.*;

/** Colours, fonts and small drawing helpers shared by the security screen. */
final class Theme {
    private Theme() { }

    /**
     * Left-to-right order of the LEDs / buttons on the screen, matching the real board:
     * button 3 on the left, button 2 in the middle, button 1 on the right.
     */
    static final int[] SCREEN_ORDER = {3, 2, 1};

    /** Screen position (0 = left, 2 = right) of LED / button 1-3. */
    static int screenPos(int led) {
        for (int i = 0; i < 3; i++) if (SCREEN_ORDER[i] == led) return i;
        return led - 1;
    }

    static final java.awt.Color BG        = new java.awt.Color(0x070B10);
    static final java.awt.Color PANEL     = new java.awt.Color(0x0C131B);
    static final java.awt.Color PANEL_2   = new java.awt.Color(0x111B26);
    static final java.awt.Color EDGE      = new java.awt.Color(0x1F3344);
    static final java.awt.Color TEXT      = new java.awt.Color(0xD6E4EE);
    static final java.awt.Color DIM       = new java.awt.Color(0x6F8799);
    static final java.awt.Color FAINT     = new java.awt.Color(0x34495A);
    static final java.awt.Color CYAN      = new java.awt.Color(0x35D6E8);
    static final java.awt.Color AMBER     = new java.awt.Color(0xFFB020);
    static final java.awt.Color ALERT     = new java.awt.Color(0xFF3B3B);
    static final java.awt.Color OK        = new java.awt.Color(0x38E07B);
    static final java.awt.Color LED_RED   = new java.awt.Color(0xFF2E2E);
    static final java.awt.Color LED_GREEN = new java.awt.Color(0x2EFF6A);
    static final java.awt.Color LED_BLUE  = new java.awt.Color(0x3A7BFF);
    static final java.awt.Color HEART     = new java.awt.Color(0xFF3355);

    static final String MONO = pick("JetBrains Mono", "Consolas", "DejaVu Sans Mono", Font.MONOSPACED);
    static final String SANS = pick("Segoe UI", "Bahnschrift", "DejaVu Sans", Font.SANS_SERIF);

    static Font mono(int style, float size) { return new Font(MONO, style, 12).deriveFont(size); }
    static Font sans(int style, float size) { return new Font(SANS, style, 12).deriveFont(size); }

    private static String pick(String... names) {
        java.util.Set<String> have = new java.util.HashSet<>(java.util.Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String n : names) if (have.contains(n)) return n;
        return names[names.length - 1];
    }

    /** Screen colour of an LED colour. */
    static java.awt.Color led(Color c) {
        if (c == null) return FAINT;
        switch (c) {
            case RED: return LED_RED;
            case GREEN: return LED_GREEN;
            case BLUE: return LED_BLUE;
            case WHITE: return java.awt.Color.WHITE;
            default: return FAINT;
        }
    }

    static java.awt.Color alpha(java.awt.Color c, int a) {
        return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static java.awt.Color mix(java.awt.Color a, java.awt.Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new java.awt.Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /** A soft round glow (for LEDs and lights). */
    static void glow(Graphics2D g, double cx, double cy, double r, java.awt.Color c, int alpha) {
        if (r <= 0) return;
        RadialGradientPaint p = new RadialGradientPaint(new Point2D.Double(cx, cy), (float) r,
                new float[]{0f, 0.35f, 1f},
                new java.awt.Color[]{alpha(c, alpha), alpha(c, alpha / 3), alpha(c, 0)});
        Paint old = g.getPaint();
        g.setPaint(p);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setPaint(old);
    }

    /** Panel frame with a title tab, like a control-room monitor. */
    static void frame(Graphics2D g, int x, int y, int w, int h, String title) {
        g.setColor(PANEL);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(EDGE);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
        if (title != null) {
            g.setFont(mono(Font.BOLD, 11f));
            g.setColor(CYAN);
            g.fillRect(x + 12, y + 12, 3, 11);
            g.setColor(DIM);
            g.drawString(title, x + 21, y + 22);
        }
        // corner ticks
        g.setColor(alpha(CYAN, 120));
        g.drawLine(x + w - 14, y + 4, x + w - 4, y + 4);
        g.drawLine(x + w - 4, y + 4, x + w - 4, y + 14);
        g.drawLine(x + 4, y + h - 14, x + 4, y + h - 4);
        g.drawLine(x + 4, y + h - 4, x + 14, y + h - 4);
    }

    static void centered(Graphics2D g, String s, double cx, double baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) baseline);
    }

    /** A heart shape inside the given box. */
    static Shape heart(double x, double y, double s) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + s / 2, y + s * 0.92);
        p.curveTo(x - s * 0.15, y + s * 0.45, x + s * 0.1, y - s * 0.1, x + s / 2, y + s * 0.25);
        p.curveTo(x + s * 0.9, y - s * 0.1, x + s * 1.15, y + s * 0.45, x + s / 2, y + s * 0.92);
        p.closePath();
        return p;
    }

    /** A shield emblem. */
    static Shape shield(double x, double y, double w, double h) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + w / 2, y);
        p.lineTo(x + w, y + h * 0.16);
        p.curveTo(x + w, y + h * 0.62, x + w * 0.78, y + h * 0.86, x + w / 2, y + h);
        p.curveTo(x + w * 0.22, y + h * 0.86, x, y + h * 0.62, x, y + h * 0.16);
        p.closePath();
        return p;
    }

    /** Arrow for a joystick direction: LEFT, RIGHT, or CENTER (up/down double arrow). */
    static Shape arrow(game.Position p, double cx, double cy, double s) {
        Path2D a = new Path2D.Double();
        double h = s / 2, t = s * 0.16;
        if (p == game.Position.CENTER) {
            a.moveTo(cx, cy - h); a.lineTo(cx + h * 0.62, cy - h * 0.3); a.lineTo(cx + t, cy - h * 0.3);
            a.lineTo(cx + t, cy + h * 0.3); a.lineTo(cx + h * 0.62, cy + h * 0.3); a.lineTo(cx, cy + h);
            a.lineTo(cx - h * 0.62, cy + h * 0.3); a.lineTo(cx - t, cy + h * 0.3); a.lineTo(cx - t, cy - h * 0.3);
            a.lineTo(cx - h * 0.62, cy - h * 0.3); a.closePath();
            return a;
        }
        double d = p == game.Position.LEFT ? -1 : 1;
        a.moveTo(cx + d * h, cy); a.lineTo(cx + d * h * 0.1, cy - h * 0.7); a.lineTo(cx + d * h * 0.1, cy - t);
        a.lineTo(cx - d * h, cy - t); a.lineTo(cx - d * h, cy + t); a.lineTo(cx + d * h * 0.1, cy + t);
        a.lineTo(cx + d * h * 0.1, cy + h * 0.7); a.closePath();
        return a;
    }
}
