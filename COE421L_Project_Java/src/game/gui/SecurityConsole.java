package game.gui;

import game.GameListener;
import game.GameManager;
import game.GameView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * The security control room window. It is a GameListener (Observer): the GameManager
 * hands it a GameView snapshot after every change, and a Swing Timer redraws the
 * animations about 30 times per second.
 */
public class SecurityConsole extends JFrame implements GameListener {
    private final GameManager game;
    private volatile GameView view;
    private final Overlay overlay;

    public SecurityConsole(GameManager game, boolean hardware, String portName) {
        super("Sentry Turret \u2014 Security Control (COE421L)");
        this.game = game;

        JPanel root = new JPanel(new BorderLayout(10, 10)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        root.add(new HeaderBar(this::view, portName), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(10, 10));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // the monitor wall: three CCTV feeds with the overlay on top
        JPanel zones = new JPanel(new GridLayout(1, 3, 10, 0));
        zones.setOpaque(false);
        for (int led : Theme.SCREEN_ORDER) zones.add(new ZoneView(led, this::view));
        overlay = new Overlay(this::view, game::startShiftPressed, this::newTeam);
        JPanel wall = new JPanel(null) {
            @Override public boolean isOptimizedDrawingEnabled() { return false; }
            @Override public void doLayout() {
                for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        wall.setOpaque(false);
        wall.add(overlay);       // index 0 = painted on top
        wall.add(zones);

        JPanel centre = new JPanel(new BorderLayout(0, 10));
        centre.setOpaque(false);
        centre.add(wall, BorderLayout.CENTER);
        centre.add(new ChallengeBar(this::view), BorderLayout.SOUTH);
        body.add(centre, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(330, 600));
        JPanel sideTop = new JPanel(new BorderLayout(0, 10));
        sideTop.setOpaque(false);
        sideTop.add(new OfficerCard(this::view), BorderLayout.NORTH);
        sideTop.add(new RadarView(this::view), BorderLayout.CENTER);
        side.add(sideTop, BorderLayout.NORTH);
        side.add(new LogView(this::view), BorderLayout.CENTER);
        body.add(side, BorderLayout.EAST);

        SimulatorPanel sim = new SimulatorPanel(this::send, hardware);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        south.add(sim, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1440, 880);
        setLocationRelativeTo(null);

        installKeys();
        new Timer(33, e -> { overlay.sync(); getContentPane().repaint(); }).start();
        game.addListener(this);
    }

    GameView view() { return view; }

    @Override
    public void gameChanged(GameView v) { view = v; }   // the timer redraws

    private void send(String line) {
        game.handleHardwareMessage(line);
    }

    /** Asks for a new team of officers (also used at start-up). */
    public void newTeam() {
        if (game.isPlaying()) return;
        java.util.List<String> names = SetupDialog.ask(this, game.getOfficerNames());
        game.setOfficers(names);
    }

    /** Keyboard simulator - works while this window has focus. */
    private void installKeys() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || !isFocused()) return false;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE: send("SHOT,2"); return true;
                case KeyEvent.VK_1: case KeyEvent.VK_NUMPAD1: send("BUTTON_PRESS,1"); return true;
                case KeyEvent.VK_2: case KeyEvent.VK_NUMPAD2: send("BUTTON_PRESS,2"); return true;
                case KeyEvent.VK_3: case KeyEvent.VK_NUMPAD3: send("BUTTON_PRESS,3"); return true;
                case KeyEvent.VK_LEFT: send("JOYSTICK,LEFT"); return true;
                case KeyEvent.VK_RIGHT: send("JOYSTICK,RIGHT"); return true;
                case KeyEvent.VK_UP: case KeyEvent.VK_DOWN: send("JOYSTICK,CENTER"); return true;
                case KeyEvent.VK_C: send("CLAP"); return true;
                default: return false;
            }
        });
    }
}
