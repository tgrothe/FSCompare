import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class FSCompare {
    static class ETableModel extends AbstractTableModel {
        final List<Object[]> table = new ArrayList<>();

        {
            addNewRow();
        }

        public void addNewRow() {
            table.add(new Object[] {"", 0f, new JProgressBar()});
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

    public FSCompare() {
        TableModel model = new ETableModel();
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
                                try {
                                    updateCell(table, editor);
                                } catch (IOException ignore) {
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
        JFrame frame = new JFrame("File Size Compare");
        frame.add(new JScrollPane(table));
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    private void updateCell(JTable table, JTextField editor) throws IOException {
        final int row = table.convertRowIndexToModel(table.getEditingRow());
        final int col = table.convertColumnIndexToModel(table.getEditingColumn());
        final int col0 = table.convertColumnIndexToView(0);
        final int col1 = table.convertColumnIndexToView(1);
        final int col2 = table.convertColumnIndexToView(2);
        if (col == 0) {
            boolean newRowNeeded = true;
            float max = 0;
            for (int i = 0; i < table.getRowCount(); i++) {
                String path = (String) table.getValueAt(i, col0);
                if (i == row) {
                    path = editor.getText();
                }
                if (new File(path).exists()) {
                    float size = Files.size(Path.of(path)) / 1024f / 1024f;
                    table.setValueAt(size, i, col1);
                    if (size > max) {
                        max = size;
                    }
                } else {
                    table.setValueAt(0f, i, col1);
                    newRowNeeded = false;
                }
            }
            for (int i = 0; i < table.getRowCount(); i++) {
                float size = (float) table.getValueAt(i, col1);
                int percent = (int) (size / max * 100f);
                ((JProgressBar) table.getValueAt(i, col2)).setValue(percent);
            }
            if (newRowNeeded && row + 1 == table.getRowCount()) {
                ((ETableModel) table.getModel()).addNewRow();
            }
            ((AbstractTableModel) table.getModel()).fireTableDataChanged();
        }
    }

    public static void main(String[] args) {
        new FSCompare();
    }
}
