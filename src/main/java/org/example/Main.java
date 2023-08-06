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
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Main {
    private static void calc(JTextField f1, JTextField f2, JTextField f3, JTextField f4) {
        SwingUtilities.invokeLater(
                () -> {
                    try {
                        String p1 = f1.getText();
                        String p2 = f2.getText();
                        if (new File(p1).exists() && new File(p2).exists()) {
                            double s1 = Files.size(Path.of(p1));
                            double s2 = Files.size(Path.of(p2));
                            f3.setText(
                                    (float) (s1 / 1024.0 / 1024.0)
                                            + " vs "
                                            + (float) (s2 / 1024.0 / 1024.0));
                            f4.setText((float) ((-1.0 + s2 / s1) * 100.0) + " %");
                        }
                    } catch (IOException ignore) {
                    }
                });
    }

    private static boolean processClipboardFail(Clipboard clipboard, JTextField field) {
        try {
            if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                Object o = clipboard.getData(DataFlavor.javaFileListFlavor);
                if (o instanceof java.util.List<?> paths) {
                    if (!paths.isEmpty()) {
                        File f = (File) paths.get(0);
                        if (f != null) {
                            // finally we got the filename :)
                            String fn = f.getAbsolutePath();
                            field.setText(fn);
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
        JTextField input1 = new JTextField("pfad1 oder strg+v");
        JTextField input2 = new JTextField("pfad2 oder strg+v");
        JButton button1 = new JButton("Auswahl 1");
        JButton button2 = new JButton("Auswahl 2");
        JTextField output1 = new JTextField();
        JTextField output2 = new JTextField();
        JPanel panel1 = new JPanel(new BorderLayout());
        JPanel panel2 = new JPanel(new GridLayout(4, 1));
        JPanel panel3 = new JPanel(new GridLayout(4, 1));
        panel2.add(input1);
        panel2.add(input2);
        panel2.add(output1);
        panel2.add(output2);
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
                        calc(input1, input2, output1, output2);
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
        input1.getDocument().addDocumentListener(dl);
        input2.getDocument().addDocumentListener(dl);

        Function<JTextField, ActionListener> alFactory1 =
                inputField ->
                        e -> {
                            JFileChooser jfc = new JFileChooser();
                            jfc.showOpenDialog(null);
                            if (jfc.getSelectedFile() != null) {
                                inputField.setText(jfc.getSelectedFile().getAbsolutePath());
                            }
                        };
        button1.addActionListener(alFactory1.apply(input1));
        button2.addActionListener(alFactory1.apply(input2));

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        KeyStroke ctrlV = KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK);
        ActionListener regularAction1 = input1.getActionForKeyStroke(ctrlV);
        ActionListener regularAction2 = input2.getActionForKeyStroke(ctrlV);
        BiFunction<JTextField, ActionListener, ActionListener> alFactor2 =
                (inputField, regularAction) ->
                        e -> {
                            if (processClipboardFail(clipboard, inputField)) {
                                regularAction1.actionPerformed(e);
                            }
                        };

        input1.registerKeyboardAction(
                alFactor2.apply(input1, regularAction1), ctrlV, JComponent.WHEN_FOCUSED);
        input2.registerKeyboardAction(
                alFactor2.apply(input2, regularAction2), ctrlV, JComponent.WHEN_FOCUSED);
    }
}
