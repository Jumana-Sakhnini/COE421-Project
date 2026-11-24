package game;

import game.gui.SecurityConsole;

import javax.swing.*;

/**
 * COE421L Sentry Turret - entry point.
 *
 * Simulation : no program arguments. Play with the on-screen simulator or the keyboard
 *              (SPACE fire, 1 2 3 buttons, arrows joystick, C clap).
 * Hardware   : Program arguments  --connect
 *              VM arguments       -Dgame.serial.port=COM8   (the USB XBee's COM port)
 *
 * The window asks for the number of security officers and their names. Each officer
 * presses START SHIFT on the screen to begin a shift; the shift ends only when all 3 hearts
 * are lost. After the last officer the leaderboard is shown.
 */
public class Main {
    public static void main(String[] args) {
        GameManager game = GameManager.getInstance();
        boolean hardware = args.length > 0 && args[0].equalsIgnoreCase("--connect");
        String port = System.getProperty("game.serial.port", "COM8");

        if (hardware) {
            if (game.getCommunicationManager().connect()) {
                // the listener thread passes every LilyPad line to the game
                game.getCommunicationManager().startListening(game::handleHardwareMessage);
            } else {
                System.out.println("Could not open " + port + " - starting in simulation mode.");
                hardware = false;
            }
        }
        final boolean hw = hardware;

        SwingUtilities.invokeLater(() -> {
            SecurityConsole console = new SecurityConsole(game, hw, port);
            console.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent e) {
                    game.shutdown();
                    game.getCommunicationManager().disconnect();
                    System.exit(0);
                }
            });
            console.setVisible(true);
            if (!hw) JOptionPane.showMessageDialog(console,
                    "No XBee connected - SIMULATION MODE.\n\nUse the TRAINING SIMULATOR buttons or the keyboard:\n"
                    + "SPACE = fire   1 2 3 = buttons   \u2190 \u2191\u2193 \u2192 = joystick   C = clap\n\n"
                    + "For the real board run with:  --connect   and   -Dgame.serial.port=COMx",
                    "Sentry Turret", JOptionPane.INFORMATION_MESSAGE);
            console.newTeam();
        });
    }
}
