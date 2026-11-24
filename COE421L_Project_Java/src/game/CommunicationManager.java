package game;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * The XBee link to the LilyPad, using jSSC (jssc-2.9.6.jar) like Lab 09's
 * SerialPortHandle. The USB XBee (Explorer) appears as a COM port; everything sent
 * to it reaches the LilyPad's XBee, and everything the LilyPad sends comes back here.
 *
 * Messages are plain text lines ending with '\n', e.g. "LIGHT,2,RED" (to the LilyPad)
 * and "BUTTON_PRESS,3" (from the LilyPad).
 *
 * Without a connection (simulation mode) commands are just printed.
 */
public class CommunicationManager {
    private final String portName;
    private final int baudRate;
    private SerialPort serialPort;
    private volatile boolean listening;
    private Thread listenerThread;

    public CommunicationManager(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    /** Opens the port: 9600 baud, 8 data bits, 1 stop bit, no parity (same as the XBees). */
    public boolean connect() {
        try {
            serialPort = new SerialPort(portName);
            serialPort.openPort();
            serialPort.setParams(baudRate, SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            System.out.println("Connected to " + portName);
            return true;
        } catch (SerialPortException e) {
            System.out.println("Could not open " + portName + ": " + e.getExceptionType());
            serialPort = null;
            return false;
        }
    }

    public boolean isConnected() { return serialPort != null && serialPort.isOpened(); }

    /**
     * Starts the "Arduino-Serial-Listener" thread. It collects incoming bytes, cuts
     * them into lines and hands every complete line to messageHandler
     * (GameManager::handleHardwareMessage).
     */
    public void startListening(Consumer<String> messageHandler) {
        if (!isConnected() || listening) return;
        listening = true;
        listenerThread = new Thread(() -> {
            StringBuilder incoming = new StringBuilder();
            while (listening && isConnected()) {
                try {
                    byte[] data = serialPort.readBytes();          // null when nothing arrived
                    if (data != null) {
                        incoming.append(new String(data, StandardCharsets.US_ASCII));
                        int newline;
                        while ((newline = incoming.indexOf("\n")) >= 0) {
                            String line = incoming.substring(0, newline).trim();
                            incoming.delete(0, newline + 1);
                            if (!line.isEmpty()) {
                                System.out.println("[RX] " + line);
                                messageHandler.accept(line);
                            }
                        }
                    }
                    Thread.sleep(5);
                } catch (SerialPortException e) {
                    // keep listening: a read error must not stop the game
                    System.out.println("Serial read error: " + e.getExceptionType() + " - retrying");
                    try { Thread.sleep(200); } catch (InterruptedException ie) { break; }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Arduino-Serial-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /** Sends one command as a text line (or prints it in simulation mode). */
    public synchronized void sendCommand(HardwareCommand command) {
        command.execute();
        String message = command.toProtocolMessage();
        if (isConnected()) {
            try {
                serialPort.writeBytes((message + "\n").getBytes(StandardCharsets.US_ASCII));
            } catch (SerialPortException e) {
                System.out.println("Send failed: " + e.getExceptionType());
            }
        } else {
            System.out.println("[SIMULATION TX] " + message);
        }
    }

    public void disconnect() {
        listening = false;
        if (listenerThread != null) listenerThread.interrupt();
        if (isConnected()) {
            try {
                serialPort.closePort();
                System.out.println("Serial connection closed.");
            } catch (SerialPortException ignored) { }
        }
    }
}
