package game.gui;

import game.Color;
import game.GameView;
import game.Position;
import game.ZoneMode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Random;
import java.util.function.Supplier;

/**
 * One CCTV monitor = one target on the board (LED 1 / 2 / 3).
 * Zone A = LED 1 (left), Zone B = LED 2 (middle, IR target), Zone C = LED 3 (right).
 */
class ZoneView extends JComponent {
    private static final String[] NAMES = {"NORTH GATE", "VAULT CORRIDOR", "SERVER ROOM"};
    private final int led;                    // 1..3
    private final Supplier<GameView> source;
    private final Random noise = new Random();

    ZoneView(int led, Supplier<GameView> source) {
        this.led = led;
        this.source = source;
        setPreferredSize(new Dimension(300, 300));
    }

    /** Colour this zone's real LED has right now (what the hardware shows). */
    static Color ledColour(GameView v, int led, long now) {
        switch (v.mode) {
            case SCANNING:  return Sweep.led(v, now) == led ? Sweep.colour(v, now) : Color.OFF;
            case DECOY:     return v.decoyLed == led && now - v.modeStart < 150 ? v.decoyColor : Color.OFF;
            case CODE_SHOW:
                if (v.patternShowIndex >= 0 && v.patternShowIndex < v.pattern.size()
                        && ledOf(v.pattern.get(v.patternShowIndex)) == led) return Color.GREEN;
                return Color.OFF;
            case RESPOND:
                if (v.challengeColor == Color.GREEN) return Color.OFF;
                return v.challengeLed == led ? v.challengeColor : Color.OFF;
            default:        return Color.OFF;
        }
    }

    static int ledOf(Position p) { return p == Position.LEFT ? 1 : p == Position.CENTER ? 2 : 3; }

    @Override
    protected void paintComponent(Graphics g0) {
        GameView v = source.get();
        if (v == null) return;
        long now = System.currentTimeMillis();
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        Shape screen = new RoundRectangle2D.Double(0, 0, w, h, 12, 12);
        g.setClip(screen);

        Color lit = ledColour(v, led, now);
        java.awt.Color light = Theme.led(lit);
        boolean on = lit != Color.OFF;
        boolean down = v.mode == ZoneMode.OFFICER_DOWN;

        // ---- room
        g.setPaint(new GradientPaint(0, 0, new java.awt.Color(0x0E1E22), 0, h, new java.awt.Color(0x04090B)));
        g.fillRect(0, 0, w, h);
        drawRoom(g, w, h);

        // ---- lamp light from the ceiling (= the real LED)
        if (on) {
            double strength = v.mode == ZoneMode.DECOY ? 0.35 : 1.0;
            if (v.mode == ZoneMode.SCANNING) strength *= 0.65 + 0.35 * Math.sin(Sweep.frac(v, now) * Math.PI);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, h * 0.06), (float) (h * 0.95),
                    new float[]{0f, 0.5f, 1f},
                    new java.awt.Color[]{Theme.alpha(light, (int) (150 * strength)),
                            Theme.alpha(light, (int) (55 * strength)), Theme.alpha(light, 0)}));
            g.fillRect(0, 0, w, h);
            // light cone
            Path2D cone = new Path2D.Double();
            cone.moveTo(w * 0.44, h * 0.09); cone.lineTo(w * 0.56, h * 0.09);
            cone.lineTo(w * 0.86, h * 0.92); cone.lineTo(w * 0.14, h * 0.92); cone.closePath();
            g.setPaint(new GradientPaint(0, (float) (h * 0.09), Theme.alpha(light, (int) (90 * strength)),
                    0, (float) (h * 0.92), Theme.alpha(light, 0)));
            g.fill(cone);
        }
        drawLamp(g, w, h, light, on);

        // ---- what is happening in this zone
        switch (v.mode) {
            case SCANNING: drawScanBeam(g, v, w, h, now); break;
            case DECOY:
                if (v.decoyLed == led) drawGhost(g, w, h, now - v.modeStart);
                break;
            case CODE_SHOW:
                if (on) drawCodeArrow(g, v, w, h);
                else caption(g, w, h, "ACCESS CODE", Theme.alpha(Theme.LED_GREEN, 140));
                break;
            case RESPOND:
                if (v.challengeColor == Color.RED && v.challengeLed == led) drawIntruder(g, w, h, now);
                else if (v.challengeColor == Color.BLUE && v.challengeLed == led) drawBreach(g, w, h, now);
                else if (v.challengeColor == Color.GREEN) drawKeypad(g, w, h, now);
                else caption(g, w, h, "CLEAR", Theme.alpha(Theme.DIM, 150));
                break;
            case SECURED:
                if (v.resultLed == 0 || v.resultLed == led) stamp(g, w, h, "SECURED", Theme.OK);
                break;
            case FAILED:
                if (v.resultLed == 0 || v.resultLed == led) stamp(g, w, h, "BREACHED", Theme.ALERT);
                break;
            default:
        }

        if (down) drawStatic(g, w, h);

        // ---- wrong action: red alarm flash on every monitor
        long sinceWrong = now - v.lastWrong;
        if (v.lastWrong > 0 && sinceWrong < 450) {
            g.setColor(Theme.alpha(Theme.ALERT, (int) (110 * (1 - sinceWrong / 450.0))));
            g.fillRect(0, 0, w, h);
        }

        drawCameraOverlay(g, v, w, h, now, light, on);
        g.setClip(null);

        // ---- monitor bezel; flashing frame while this zone needs action
        boolean alarm = v.mode == ZoneMode.RESPOND && v.challengeColor != Color.GREEN && v.challengeLed == led;
        java.awt.Color edge = alarm ? Theme.alpha(light, (now / 250) % 2 == 0 ? 255 : 120) : Theme.EDGE;
        g.setStroke(new BasicStroke(alarm ? 3f : 1.4f));
        g.setColor(edge);
        g.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 12, 12));
        g.dispose();
    }

    // ------------------------------------------------------------------ scene
    private void drawRoom(Graphics2D g, int w, int h) {
        double bx = w * 0.27, by = h * 0.24, bw = w * 0.46, bh = h * 0.42;
        java.awt.Color line = new java.awt.Color(0x1C3A40);
        g.setStroke(new BasicStroke(1f));
        // back wall
        g.setColor(new java.awt.Color(0x0B181B));
        g.fill(new Rectangle2D.Double(bx, by, bw, bh));
        g.setColor(line);
        g.draw(new Rectangle2D.Double(bx, by, bw, bh));
        g.draw(new Line2D.Double(0, 0, bx, by));
        g.draw(new Line2D.Double(w, 0, bx + bw, by));
        g.draw(new Line2D.Double(0, h, bx, by + bh));
        g.draw(new Line2D.Double(w, h, bx + bw, by + bh));
        // floor grid
        g.setColor(new java.awt.Color(0x14292D));
        for (int i = 1; i < 8; i++) {
            double t = i / 8.0;
            g.draw(new Line2D.Double(bx + bw * t, by + bh, w * t, h));
        }
        for (int i = 1; i < 6; i++) {
            double t = Math.pow(i / 6.0, 1.7);
            double y = by + bh + (h - by - bh) * t;
            double xl = bx * (1 - t), xr = bx + bw + (w - bx - bw) * t;
            g.draw(new Line2D.Double(xl, y, xr, y));
        }
        // props
        g.setColor(new java.awt.Color(0x1F4148));
        if (led == 1) {                       // gate with bars
            double gx = bx + bw * 0.28, gw = bw * 0.44, gy = by + bh * 0.18, gh = bh * 0.82;
            g.draw(new Rectangle2D.Double(gx, gy, gw, gh));
            for (int i = 1; i < 6; i++) g.draw(new Line2D.Double(gx + gw * i / 6, gy, gx + gw * i / 6, gy + gh));
            g.draw(new Line2D.Double(gx, gy + gh * 0.5, gx + gw, gy + gh * 0.5));
        } else if (led == 2) {                // vault door
            double r = bh * 0.36, cx = bx + bw / 2, cy = by + bh * 0.55;
            g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            g.draw(new Ellipse2D.Double(cx - r * 0.7, cy - r * 0.7, r * 1.4, r * 1.4));
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                g.draw(new Line2D.Double(cx, cy, cx + Math.cos(a) * r * 0.7, cy + Math.sin(a) * r * 0.7));
            }
        } else {                              // server racks
            for (int k = 0; k < 3; k++) {
                double rx = bx + bw * (0.1 + k * 0.3), rw = bw * 0.22, ry = by + bh * 0.12, rh = bh * 0.88;
                g.draw(new Rectangle2D.Double(rx, ry, rw, rh));
                for (int j = 1; j < 7; j++) g.draw(new Line2D.Double(rx, ry + rh * j / 7, rx + rw, ry + rh * j / 7));
            }
            long t = System.currentTimeMillis() / 300;
            for (int k = 0; k < 3; k++) for (int j = 0; j < 6; j++)
                if (((t + k * 7 + j * 3) % 5) == 0) {
                    g.setColor(Theme.alpha(Theme.OK, 160));
                    g.fill(new Rectangle2D.Double(bx + bw * (0.12 + k * 0.3), by + bh * 0.12 + bh * 0.88 * (j + 0.4) / 7, 3, 2));
                }
        }
    }

    private void drawLamp(Graphics2D g, int w, int h, java.awt.Color light, boolean on) {
        double cx = w / 2.0, cy = h * 0.075;
        g.setColor(new java.awt.Color(0x22343B));
        g.fill(new RoundRectangle2D.Double(cx - 26, cy - 9, 52, 12, 6, 6));
        if (on) Theme.glow(g, cx, cy + 2, 40, light, 230);
        g.setColor(on ? Theme.mix(light, java.awt.Color.WHITE, 0.45) : new java.awt.Color(0x2B3B42));
        g.fill(new Ellipse2D.Double(cx - 9, cy - 5, 18, 12));
    }

    private void drawScanBeam(Graphics2D g, GameView v, int w, int h, long now) {
        // a scan bar that crosses each monitor in the same direction the sweep moves on screen
        if (Sweep.led(v, now) != led) {
            caption(g, w, h, "SCANNING", Theme.alpha(Theme.DIM, 120));
            return;
        }
        // the sweep goes LED 3 -> 2 -> 1; on screen that is to the right if LED 3 is on the left
        boolean toRight = Theme.screenPos(3) < Theme.screenPos(1);
        double f = Sweep.frac(v, now);
        double x = toRight ? w * f : w * (1 - f);
        double tail = toRight ? -w * 0.35 : w * 0.35;       // fading trail behind the bar
        java.awt.Color c = Theme.led(Sweep.colour(v, now));
        g.setPaint(new GradientPaint((float) x, 0, Theme.alpha(c, 170), (float) (x + tail), 0, Theme.alpha(c, 0)));
        g.fill(new Rectangle2D.Double(Math.min(x, x + tail), 0, Math.abs(tail), h));
        g.setColor(Theme.alpha(c, 230));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(x, 0, x, h));
        caption(g, w, h, toRight ? "SCANNING  \u25B6" : "SCANNING  \u25C0", Theme.alpha(c, 230));
    }

    private Shape person(double cx, double feet, double size) {
        Area a = new Area(new Ellipse2D.Double(cx - size * 0.11, feet - size, size * 0.22, size * 0.24));
        Path2D body = new Path2D.Double();
        body.moveTo(cx - size * 0.2, feet - size * 0.74);
        body.curveTo(cx - size * 0.3, feet - size * 0.72, cx - size * 0.31, feet - size * 0.5, cx - size * 0.3, feet - size * 0.3);
        body.lineTo(cx - size * 0.2, feet - size * 0.3);
        body.lineTo(cx - size * 0.17, feet);
        body.lineTo(cx - size * 0.03, feet);
        body.lineTo(cx, feet - size * 0.36);
        body.lineTo(cx + size * 0.03, feet);
        body.lineTo(cx + size * 0.17, feet);
        body.lineTo(cx + size * 0.2, feet - size * 0.3);
        body.lineTo(cx + size * 0.3, feet - size * 0.3);
        body.curveTo(cx + size * 0.31, feet - size * 0.5, cx + size * 0.3, feet - size * 0.72, cx + size * 0.2, feet - size * 0.74);
        body.closePath();
        a.add(new Area(body));
        return a;
    }

    private void drawIntruder(Graphics2D g, int w, int h, long now) {
        double size = h * 0.5, cx = w / 2.0 + Math.sin(now / 420.0) * w * 0.04, feet = h * 0.8;
        Shape p = person(cx, feet, size);
        g.setColor(new java.awt.Color(0x05080A));
        g.fill(p);
        g.setColor(Theme.alpha(Theme.LED_RED, 220));
        g.setStroke(new BasicStroke(2f));
        g.draw(p);
        // crosshair locked on the chest
        double ty = feet - size * 0.58, r = Math.min(w, h) * 0.13 + Math.sin(now / 120.0) * 3;
        g.setColor(Theme.alpha(Theme.LED_RED, 240));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(new Ellipse2D.Double(cx - r, ty - r, r * 2, r * 2));
        g.draw(new Line2D.Double(cx - r * 1.5, ty, cx - r * 0.4, ty));
        g.draw(new Line2D.Double(cx + r * 0.4, ty, cx + r * 1.5, ty));
        g.draw(new Line2D.Double(cx, ty - r * 1.5, cx, ty - r * 0.4));
        g.draw(new Line2D.Double(cx, ty + r * 0.4, cx, ty + r * 1.5));
        g.fill(new Ellipse2D.Double(cx - 2.5, ty - 2.5, 5, 5));
        alertBand(g, w, h, "INTRUDER \u2022 FIRE!", Theme.LED_RED, now);
    }

    private void drawBreach(Graphics2D g, int w, int h, long now) {
        double bx = w * 0.27, by = h * 0.24, bw = w * 0.46, bh = h * 0.42;
        double dx = bx + bw * 0.34, dw = bw * 0.32, dy = by + bh * 0.12, dh = bh * 0.88;
        // open doorway with blue light spilling out
        g.setColor(Theme.alpha(Theme.LED_BLUE, 200));
        g.fill(new Rectangle2D.Double(dx, dy, dw, dh));
        Path2D spill = new Path2D.Double();
        spill.moveTo(dx, dy + dh); spill.lineTo(dx + dw, dy + dh);
        spill.lineTo(dx + dw * 2.2, h); spill.lineTo(dx - dw * 1.2, h); spill.closePath();
        g.setPaint(new GradientPaint(0, (float) (dy + dh), Theme.alpha(Theme.LED_BLUE, 150), 0, h, Theme.alpha(Theme.LED_BLUE, 0)));
        g.fill(spill);
        double open = 0.55 + 0.1 * Math.sin(now / 200.0);
        Path2D door = new Path2D.Double();
        door.moveTo(dx, dy); door.lineTo(dx - dw * open, dy - dh * 0.06);
        door.lineTo(dx - dw * open, dy + dh * 1.06); door.lineTo(dx, dy + dh); door.closePath();
        g.setColor(new java.awt.Color(0x0A1420));
        g.fill(door);
        g.setColor(Theme.alpha(Theme.LED_BLUE, 230));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(door);
        // open padlock
        double lx = w / 2.0, ly = h * 0.72, s = Math.min(w, h) * 0.12;
        g.setStroke(new BasicStroke((float) (s * 0.14), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(lx - s * 0.32, ly - s * 0.95, s * 0.64, s * 0.8, 0, 180, Arc2D.OPEN));
        g.draw(new Line2D.Double(lx + s * 0.32, ly - s * 0.55, lx + s * 0.32, ly - s * 0.35));
        g.fill(new RoundRectangle2D.Double(lx - s * 0.5, ly - s * 0.45, s, s * 0.8, 6, 6));
        alertBand(g, w, h, "BREACH \u2022 BUTTON " + led, Theme.LED_BLUE, now);
    }

    private void drawCodeArrow(Graphics2D g, GameView v, int w, int h) {
        Position p = v.pattern.get(v.patternShowIndex);
        Theme.glow(g, w / 2.0, h * 0.55, Math.min(w, h) * 0.38, Theme.LED_GREEN, 140);
        g.setColor(Theme.LED_GREEN);
        g.fill(Theme.arrow(p, w / 2.0, h * 0.55, Math.min(w, h) * 0.42));
        g.setFont(Theme.mono(Font.BOLD, 13f));
        g.setColor(Theme.mix(Theme.LED_GREEN, java.awt.Color.WHITE, 0.4));
        Theme.centered(g, "CODE " + (v.patternShowIndex + 1) + "/" + v.pattern.size()
                + " : " + (p == Position.CENTER ? "UP/DOWN" : p.name()), w / 2.0, h * 0.86);
    }

    private void drawKeypad(Graphics2D g, int w, int h, long now) {
        double s = Math.min(w, h) * 0.075, cx = w / 2.0, cy = h * 0.56;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            boolean blink = ((now / 180) + r * 3 + c + led) % 9 == 0;
            g.setColor(Theme.alpha(Theme.LED_GREEN, blink ? 200 : 60));
            g.fill(new RoundRectangle2D.Double(cx + (c - 1.5) * s * 1.3 + s * 0.15, cy + (r - 1.5) * s * 1.3 + s * 0.15, s, s, 5, 5));
        }
        caption(g, w, h, "ENTER CODE \u2195", Theme.alpha(Theme.LED_GREEN, 210));
    }

    private void drawGhost(Graphics2D g, int w, int h, long t) {
        if (t > 900) return;
        int a = (int) (90 * (1 - t / 900.0) * (0.5 + 0.5 * Math.sin(t / 30.0)));
        Shape p = person(w / 2.0, h * 0.8, h * 0.5);
        g.setColor(Theme.alpha(Theme.TEXT, a));
        g.fill(p);
        caption(g, w, h, "?? FAINT SIGNAL", Theme.alpha(Theme.AMBER, 90 + a));
    }

    private void drawStatic(Graphics2D g, int w, int h) {
        for (int i = 0; i < 900; i++) {
            int x = noise.nextInt(w), y = noise.nextInt(h), c = 40 + noise.nextInt(160);
            g.setColor(new java.awt.Color(c, c, c, 150));
            g.fillRect(x, y, 2, 2);
        }
        g.setFont(Theme.mono(Font.BOLD, 13f));
        g.setColor(Theme.ALERT);
        Theme.centered(g, "SIGNAL LOST", w / 2.0, h * 0.9);
    }

    private void stamp(Graphics2D g, int w, int h, String text, java.awt.Color c) {
        Graphics2D s = (Graphics2D) g.create();
        s.rotate(Math.toRadians(-12), w / 2.0, h / 2.0);
        s.setFont(Theme.mono(Font.BOLD, Math.max(18f, w / 9f)));
        FontMetrics fm = s.getFontMetrics();
        int tw = fm.stringWidth(text), th = fm.getAscent();
        double x = w / 2.0 - tw / 2.0 - 14, y = h / 2.0 - th / 2.0 - 8;
        s.setColor(Theme.alpha(java.awt.Color.BLACK, 140));
        s.fill(new RoundRectangle2D.Double(x, y, tw + 28, th + 18, 8, 8));
        s.setColor(c);
        s.setStroke(new BasicStroke(3f));
        s.draw(new RoundRectangle2D.Double(x, y, tw + 28, th + 18, 8, 8));
        s.drawString(text, (float) (x + 14), (float) (y + 9 + th * 0.9));
        s.dispose();
    }

    private void alertBand(Graphics2D g, int w, int h, String text, java.awt.Color c, long now) {
        double y = h * 0.86, bh = h * 0.09;
        g.setColor(Theme.alpha(java.awt.Color.BLACK, 170));
        g.fill(new Rectangle2D.Double(0, y, w, bh));
        // hazard stripes
        Shape old = g.getClip();
        g.clip(new Rectangle2D.Double(0, y, w, bh));
        g.setColor(Theme.alpha(c, 70));
        double off = (now / 25.0) % 24;
        for (double x = -bh - 24 + off; x < w + bh; x += 24) {
            Path2D st = new Path2D.Double();
            st.moveTo(x, y + bh); st.lineTo(x + 10, y + bh); st.lineTo(x + 10 + bh, y); st.lineTo(x + bh, y); st.closePath();
            g.fill(st);
        }
        g.setClip(old);
        float fs = (float) Math.max(10, bh * 0.5);
        g.setFont(Theme.mono(Font.BOLD, fs));
        while (fs > 9 && g.getFontMetrics().stringWidth(text) > w - 24) g.setFont(Theme.mono(Font.BOLD, fs -= 1f));
        g.setColor((now / 300) % 2 == 0 ? java.awt.Color.WHITE : Theme.mix(c, java.awt.Color.WHITE, 0.5));
        Theme.centered(g, text, w / 2.0, y + bh * 0.68);
    }

    private void caption(Graphics2D g, int w, int h, String text, java.awt.Color c) {
        g.setFont(Theme.mono(Font.BOLD, 12f));
        g.setColor(c);
        Theme.centered(g, text, w / 2.0, h * 0.9);
    }

    private void drawCameraOverlay(Graphics2D g, GameView v, int w, int h, long now, java.awt.Color light, boolean on) {
        // scanlines + a slow rolling bar
        g.setColor(new java.awt.Color(0, 0, 0, 38));
        for (int y = 0; y < h; y += 3) g.drawLine(0, y, w, y);
        double roll = (now / 14.0) % (h + 60) - 60;
        g.setPaint(new GradientPaint(0, (float) roll, new java.awt.Color(255, 255, 255, 0), 0, (float) roll + 30,
                new java.awt.Color(255, 255, 255, 12), true));
        g.fill(new Rectangle2D.Double(0, roll, w, 60));
        // vignette
        g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, h / 2.0), (float) (Math.max(w, h) * 0.75),
                new float[]{0.55f, 1f}, new java.awt.Color[]{new java.awt.Color(0, 0, 0, 0), new java.awt.Color(0, 0, 0, 170)}));
        g.fillRect(0, 0, w, h);

        // labels
        g.setFont(Theme.mono(Font.BOLD, 12f));
        g.setColor(Theme.TEXT);
        g.drawString("CAM 0" + led + "  \u25B8  ZONE " + (char) ('A' + led - 1), 12, 22);
        g.setFont(Theme.mono(Font.PLAIN, 10.5f));
        g.setColor(Theme.DIM);
        g.drawString(NAMES[led - 1], 12, 37);
        if ((now / 600) % 2 == 0) {
            g.setColor(Theme.ALERT);
            g.fill(new Ellipse2D.Double(w - 52, 13, 9, 9));
        }
        g.setFont(Theme.mono(Font.BOLD, 11f));
        g.setColor(Theme.TEXT);
        g.drawString("REC", w - 38, 22);
        g.setFont(Theme.mono(Font.PLAIN, 10f));
        g.setColor(Theme.DIM);
        g.drawString(java.time.LocalTime.now().withNano(0).toString(), w - 62, 37);

        // the physical LED + what answers it
        double lx = 20, ly = h - 18;
        if (on) Theme.glow(g, lx, ly, 18, light, 220);
        g.setColor(on ? Theme.mix(light, java.awt.Color.WHITE, 0.35) : new java.awt.Color(0x24343C));
        g.fill(new Ellipse2D.Double(lx - 6, ly - 6, 12, 12));
        g.setColor(Theme.DIM);
        g.setFont(Theme.mono(Font.PLAIN, 10f));
        g.drawString("LED " + led + (led == 2 ? " \u00B7 IR TARGET" : "") + " \u00B7 BTN " + led, 33, (float) ly + 4);
    }
}
