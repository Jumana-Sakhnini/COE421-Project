package game.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Flat control-room button. */
class ConsoleButton extends JButton {
    private final java.awt.Color accent;
    private boolean hover, down;

    ConsoleButton(String text, java.awt.Color accent) {
        super(text);
        this.accent = accent;
        setFont(Theme.mono(Font.BOLD, 12f));
        setForeground(Theme.TEXT);
        setFocusable(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(6, 12, 6, 12));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; down = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { down = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { down = false; repaint(); }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 28, 34);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        g.setColor(down ? Theme.alpha(accent, 110) : hover ? Theme.alpha(accent, 55) : Theme.PANEL_2);
        g.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
        g.setColor(Theme.alpha(accent, hover ? 255 : 160));
        g.setStroke(new BasicStroke(1.3f));
        g.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
        g.fillRect(6, h - 3, w - 12, 2);
        g.setFont(getFont());
        g.setColor(isEnabled() ? getForeground() : Theme.FAINT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h + fm.getAscent() - fm.getDescent()) / 2 - 1);
        g.dispose();
    }
}
