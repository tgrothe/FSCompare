import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class FSCompare {
    class ETableModel extends AbstractTableModel {
        final List<Object[]> table = new ArrayList<>();

        {
            addNewRow();
        }

        public void addNewRow() {
            table.add(new Object[] {"", 0f, new JProgressBar()});
        }

        public void addDir() throws IOException {
            final int row = currentRow;
            final int col0 = 0;
            String path = (String) getValueAt(row, col0);
            File dir = new File(path);
            if (dir.canRead() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                assert files != null;
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (int i = 0; i < files.length; i++) {
                    int ri = row + i + 1;
                    setValueAt(files[i].getPath(), ri, col0);
                    if (ri + 1 == getRowCount()) {
                        addNewRow();
                    }
                }
                calculateTable();
            }
        }

        public void calculateTable() throws IOException {
            final int col0 = 0;
            final int col1 = 1;
            final int col2 = 2;
            float max = 0;
            for (int i = 0; i < getRowCount(); i++) {
                String path = (String) getValueAt(i, col0);
                if (new File(path).canRead()) {
                    float size = Files.size(Path.of(path)) / 1024f / 1024f;
                    setValueAt(size, i, col1);
                    if (size > max) {
                        max = size;
                    }
                } else {
                    setValueAt(0f, i, col1);
                }
            }
            for (int i = 0; i < getRowCount(); i++) {
                float size = (float) getValueAt(i, col1);
                int percent = (int) (size / max * 100f);
                ((JProgressBar) getValueAt(i, col2)).setValue(percent);
            }
            fireTableDataChanged();
        }

        public void calculateTable(JTextField editor) throws IOException {
            final int row = currentRow;
            final int col0 = 0;
            final int col1 = 1;
            final int col2 = 2;
            boolean newRowNeeded = true;
            float max = 0;
            for (int i = 0; i < getRowCount(); i++) {
                String path = (String) getValueAt(i, col0);
                if (i == row) {
                    path = editor.getText();
                }
                if (new File(path).canRead()) {
                    float size = Files.size(Path.of(path)) / 1024f / 1024f;
                    setValueAt(size, i, col1);
                    if (size > max) {
                        max = size;
                    }
                } else {
                    setValueAt(0f, i, col1);
                    newRowNeeded = false;
                }
            }
            for (int i = 0; i < getRowCount(); i++) {
                float size = (float) getValueAt(i, col1);
                int percent = (int) (size / max * 100f);
                ((JProgressBar) getValueAt(i, col2)).setValue(percent);
            }
            if (newRowNeeded && row + 1 == getRowCount()) {
                addNewRow();
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return table.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return table.get(rowIndex)[columnIndex];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            table.get(rowIndex)[columnIndex] = aValue;
        }

        @Override
        public String getColumnName(int column) {
            return new String[] {"Path", "Size in MiB", "Percent"}[column];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }
    }

    private volatile int currentRow = 0;

    public FSCompare() {
        ETableModel model = new ETableModel();
        JTable table = new JTable(model);
        table.setDefaultRenderer(
                Object.class,
                new TableCellRenderer() {
                    static class ProgressRenderer extends JProgressBar
                            implements TableCellRenderer {
                        public ProgressRenderer(JProgressBar progressBar) {
                            this.setValue(progressBar.getValue());
                            this.setStringPainted(true);
                        }

                        @Override
                        public Component getTableCellRendererComponent(
                                JTable table,
                                Object value,
                                boolean isSelected,
                                boolean hasFocus,
                                int row,
                                int column) {
                            return this;
                        }
                    }

                    final DefaultTableCellRenderer defaultTableCellRenderer =
                            new DefaultTableCellRenderer();

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean isSelected,
                            boolean hasFocus,
                            int row,
                            int column) {
                        if (table.convertColumnIndexToModel(column) == 2) {
                            return new ProgressRenderer(
                                            (JProgressBar) table.getValueAt(row, column))
                                    .getTableCellRendererComponent(
                                            table, value, isSelected, hasFocus, row, column);
                        }
                        return defaultTableCellRenderer.getTableCellRendererComponent(
                                table, value, isSelected, hasFocus, row, column);
                    }
                });

        DefaultCellEditor dce = (DefaultCellEditor) table.getDefaultEditor(Object.class);
        JTextField editor = (JTextField) dce.getComponent();
        editor.getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            private void update() {
                                final int col =
                                        table.convertColumnIndexToModel(table.getEditingColumn());
                                if (col == 0) {
                                    try {
                                        model.calculateTable(editor);
                                    } catch (IOException ignore) {
                                    }
                                }
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
                        });

        table.getSelectionModel()
                .addListSelectionListener(
                        x -> {
                            final int row = x.getFirstIndex();
                            if (row >= 0) {
                                currentRow = table.convertRowIndexToModel(row);
                                System.out.println("currentRow = " + currentRow);
                            }
                        });

        table.addMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                            try {
                                System.out.println("e = " + e);
                                model.addDir();
                            } catch (IOException ignore) {
                            }
                        }
                    }
                });

        JFrame frame = new JFrame("File Size Compare");
        frame.add(new JLabel("Double-click a row to add the entire folder."), BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new FSCompare();
    }
}
