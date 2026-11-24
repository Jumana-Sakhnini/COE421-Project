package game.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Asks for the number of security officers and their names. */
class SetupDialog extends JDialog {
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(2, 1, 8, 1));
    private final JPanel names = new JPanel();
    private final List<JTextField> fields = new ArrayList<>();
    private List<String> result;

    SetupDialog(Frame owner, List<String> previous) {
        super(owner, "Security Team Setup", true);
        if (previous != null && !previous.isEmpty()) count.setValue(Math.min(8, previous.size()));
        JPanel root = new JPanel(new BorderLayout(0, 14)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        JLabel t = new JLabel("SECURITY TEAM SETUP");
        t.setFont(Theme.sans(Font.BOLD, 22f));
        t.setForeground(Theme.TEXT);
        JLabel s = new JLabel("How many security officers are on this shift?");
        s.setFont(Theme.mono(Font.PLAIN, 12f));
        s.setForeground(Theme.DIM);
        top.add(t, BorderLayout.NORTH);
        top.add(s, BorderLayout.CENTER);
        JPanel cnt = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        cnt.setOpaque(false);
        count.setFont(Theme.mono(Font.BOLD, 18f));
        ((JSpinner.DefaultEditor) count.getEditor()).getTextField().setColumns(3);
        cnt.add(count);
        top.add(cnt, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        names.setOpaque(false);
        names.setLayout(new GridLayout(0, 1, 0, 6));
        root.add(names, BorderLayout.CENTER);

        ConsoleButton ok = new ConsoleButton("DEPLOY TEAM  \u25B6", Theme.OK);
        ok.addActionListener(e -> finish());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(ok);
        root.add(bottom, BorderLayout.SOUTH);

        count.addChangeListener(e -> rebuild(null));
        rebuild(previous);
        setContentPane(root);
        getRootPane().setDefaultButton(ok);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    private void rebuild(List<String> previous) {
        int n = (Integer) count.getValue();
        List<String> keep = new ArrayList<>();
        for (JTextField f : fields) keep.add(f.getText());
        if (previous != null) keep = new ArrayList<>(previous);
        names.removeAll();
        fields.clear();
        for (int i = 0; i < n; i++) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            JLabel l = new JLabel(String.format("OFFICER %d", i + 1));
            l.setFont(Theme.mono(Font.BOLD, 12f));
            l.setForeground(Theme.AMBER);
            l.setPreferredSize(new Dimension(90, 28));
            JTextField f = new JTextField(i < keep.size() ? keep.get(i) : "", 18);
            f.setFont(Theme.sans(Font.PLAIN, 15f));
            f.setBackground(Theme.PANEL_2);
            f.setForeground(Theme.TEXT);
            f.setCaretColor(Theme.CYAN);
            f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.EDGE),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            row.add(l, BorderLayout.WEST);
            row.add(f, BorderLayout.CENTER);
            names.add(row);
            fields.add(f);
        }
        pack();
        setLocationRelativeTo(getOwner());
        if (!fields.isEmpty()) SwingUtilities.invokeLater(() -> fields.get(0).requestFocusInWindow());
    }

    private void finish() {
        result = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            String n = fields.get(i).getText().trim();
            result.add(n.isEmpty() ? "Officer " + (i + 1) : n);
        }
        dispose();
    }

    /** Shows the dialog; returns the names (never empty). */
    static List<String> ask(Frame owner, List<String> previous) {
        SetupDialog d = new SetupDialog(owner, previous);
        d.setVisible(true);
        if (d.result == null || d.result.isEmpty()) {
            if (previous != null && !previous.isEmpty()) return previous;
            return List.of("Officer 1");
        }
        return d.result;
    }
}
