package org.example;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Main {
    private static void calc(
            JTextField textField1, JTextField textField2, JTextField line1, JTextField line2) {
        SwingUtilities.invokeLater(
                () -> {
                    try {
                        String p1 = textField1.getText();
                        String p2 = textField2.getText();
                        double s1 = Files.size(Path.of(p1));
                        double s2 = Files.size(Path.of(p2));
                        line1.setText(
                                (float) (s1 / 1024 / 1024) + " vs " + (float) (s2 / 1024 / 1024));
                        line2.setText((float) ((-1.0 + (s2 / s1)) * 100.0) + " %");
                    } catch (IOException ignore) {
                    }
                });
    }

    private static boolean processClipboardFail(Clipboard clipboard, JTextField textField) {
        try {
            if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                Object o = clipboard.getData(DataFlavor.javaFileListFlavor);
                if (o instanceof java.util.List<?> paths) {
                    if (!paths.isEmpty()) {
                        File f = (File) paths.get(0);
                        if (f != null) {
                            // finally, we got the filename :)
                            String fn = f.getAbsolutePath();
                            textField.setText(fn);
                            return false;
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return true;
    }

    public static void main(String[] args) {
        JTextField textField1 = new JTextField("pfad1");
        JTextField textField2 = new JTextField("pfad2");
        JButton button1 = new JButton("Auswahl 1");
        JButton button2 = new JButton("Auswahl 2");
        JTextField line1 = new JTextField();
        JTextField line2 = new JTextField();
        JPanel panel1 = new JPanel(new BorderLayout());
        JPanel panel2 = new JPanel(new GridLayout(4, 1));
        JPanel panel3 = new JPanel(new GridLayout(4, 1));
        panel2.add(textField1);
        panel2.add(textField2);
        panel2.add(line1);
        panel2.add(line2);
        panel3.add(button1);
        panel3.add(button2);
        panel1.add(panel2, BorderLayout.CENTER);
        panel1.add(panel3, BorderLayout.EAST);
        JFrame frame = new JFrame("File Size Compare");
        frame.add(panel1);
        frame.pack();
        frame.setSize(600, frame.getHeight());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        DocumentListener dl =
                new DocumentListener() {
                    private void update() {
                        calc(textField1, textField2, line1, line2);
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        update();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        update();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        update();
                    }
                };
        textField1.getDocument().addDocumentListener(dl);
        textField2.getDocument().addDocumentListener(dl);

        button1.addActionListener(
                e -> {
                    JFileChooser jfc = new JFileChooser();
                    jfc.showOpenDialog(null);
                    if (jfc.getSelectedFile() != null) {
                        textField1.setText(jfc.getSelectedFile().getAbsolutePath());
                    }
                });
        button2.addActionListener(
                e -> {
                    JFileChooser jfc = new JFileChooser();
                    jfc.showOpenDialog(null);
                    if (jfc.getSelectedFile() != null) {
                        textField2.setText(jfc.getSelectedFile().getAbsolutePath());
                    }
                });

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        KeyStroke ctrlV = KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK);
        ActionListener regularAction1 = textField1.getActionForKeyStroke(ctrlV);
        ActionListener regularAction2 = textField2.getActionForKeyStroke(ctrlV);

        textField1.registerKeyboardAction(
                e -> {
                    if (processClipboardFail(clipboard, textField1)) {
                        regularAction1.actionPerformed(e);
                    }
                },
                ctrlV,
                JComponent.WHEN_FOCUSED);
        textField2.registerKeyboardAction(
                e -> {
                    if (processClipboardFail(clipboard, textField2)) {
                        regularAction2.actionPerformed(e);
                    }
                },
                ctrlV,
                JComponent.WHEN_FOCUSED);
    }
}
