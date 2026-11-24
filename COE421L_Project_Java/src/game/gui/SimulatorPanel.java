package game.gui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Training simulator: sends the same lines the LilyPad would send, so the game can be
 * played without hardware (and tested with hardware connected).
 * Keyboard: SPACE fire, 1 2 3 buttons, LEFT / UP-DOWN / RIGHT joystick, C clap.
 */
class SimulatorPanel extends JPanel {
    SimulatorPanel(Consumer<String> send, boolean hardware) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JLabel title = new JLabel(hardware ? "MANUAL OVERRIDE" : "TRAINING SIMULATOR");
        title.setFont(Theme.mono(Font.BOLD, 12f));
        title.setForeground(hardware ? Theme.AMBER : Theme.CYAN);
        title.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));
        add(title);
        add(button("\u25CE FIRE GUN  [SPACE]", Theme.LED_RED, () -> send.accept("SHOT,2")));
        add(gap());
        for (int i : Theme.SCREEN_ORDER) {             // same left-to-right order as the board
            int id = i;
            add(button("BTN " + i + "  [" + i + "]", Theme.LED_BLUE, () -> send.accept("BUTTON_PRESS," + id)));
        }
        add(gap());
        add(button("\u25C0 LEFT", Theme.LED_GREEN, () -> send.accept("JOYSTICK,LEFT")));
        add(button("\u25B2\u25BC CENTER", Theme.LED_GREEN, () -> send.accept("JOYSTICK,CENTER")));
        add(button("RIGHT \u25B6", Theme.LED_GREEN, () -> send.accept("JOYSTICK,RIGHT")));
        add(gap());
        add(button("((\u00B7)) CLAP  [C]", Theme.CYAN, () -> send.accept("CLAP")));
    }

    private static JComponent gap() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(6, 10));
        return p;
    }

    private static JButton button(String text, java.awt.Color c, Runnable r) {
        ConsoleButton b = new ConsoleButton(text, c);
        b.addActionListener(e -> r.run());
        return b;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        Theme.frame(g, 0, 0, getWidth(), getHeight(), null);
        g.dispose();
    }
}
