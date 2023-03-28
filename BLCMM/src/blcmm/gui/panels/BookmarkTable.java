/*
 * Copyright (C) 2018-2020  LightChaosman
 *
 * OpenBLCMM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the original proprietary BLCMM Launcher, BLCMM
 * Lib Distributor, BLCMM Utilities, or BLCMM Data Interaction Library
 * Jarfiles (or modified versions of those libraries), containing parts
 * covered by the terms of their proprietary license, the licensors of
 * this Program grant you additional permission to convey the resulting
 * work.
 *
 */
package blcmm.gui.panels;

import blcmm.data.lib.DataManager;
import blcmm.data.lib.DataManager.Dump;
import blcmm.gui.ObjectExplorer;
import blcmm.gui.theme.ThemeManager;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.AbstractAction;
import javax.swing.DefaultRowSorter;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author LightChaosman
 */
public class BookmarkTable extends JTable {

    private String currentDump;
    private DataManager dm;

    public BookmarkTable(String currentDump, DataManager dm) {
        this.currentDump = currentDump;
        this.dm = dm;
        initModel();
        initRenderers();
        super.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                String objectName = getSelectedValue();
                // Get our object name of our bookmark.
                if (getSelectedColumn() == getColumnCount() - 1) {
                    BookmarkTable.this.deleteEntry(getSelectedValue());
                    return;
                }

                // Either: We double clicked w/ left mouse OR We did a single click w/ middle mouse. Both of these will dump our bookmark.
                if (objectName != null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    // Dump our bookmark
                    ObjectExplorer.INSTANCE.dump(new ObjectExplorer.DumpOptions(objectName, false));
                    BookmarkTable.this.currentDump = objectName;
                    BookmarkTable.this.repaint();
                }
            }
        });
        super.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_DELETE) {
                    deleteEntry(getSelectedValue());
                }
            }
        });
        super.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "dump");
        super.getActionMap().put("dump", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String objectName = getSelectedValue();
                ObjectExplorer.INSTANCE.dump(new ObjectExplorer.DumpOptions(objectName, false));
                BookmarkTable.this.currentDump = objectName;
                BookmarkTable.this.repaint();
            }
        });
        super.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        BookmarkTable.this.updateBookmarkBrowser();
    }

    private String getSelectedValue() {
        return getRowValue(getSelectedRow());
    }

    private String getRowValue(int row) {
        return getValueAt(row, 0) + "'" + getValueAt(row, 1) + "'";
    }

    private void deleteEntry(String entry) {
        List<String> bookmarkList = new ArrayList<>(Arrays.asList(Options.INSTANCE.getOEBookmarks(this.dm.getPatchType())));

        // Our object is currently bookmarked. Time to remove it.
        boolean wasInList = bookmarkList.remove(entry);
        Options.INSTANCE.setOEBookmarks(bookmarkList.toArray(new String[0]), this.dm.getPatchType());
        updateBookmarkBrowser();

        // We unbookmarked something. Possibly unfill our star.
        JTabbedPane tabbedPane = ObjectExplorer.INSTANCE.getObjectExplorerTabbedPane();
        IntStream.range(0, tabbedPane.getTabCount() - 1)
                .mapToObj(i -> (ObjectExplorerPanel) tabbedPane.getComponentAt(i))
                .forEach(ObjectExplorerPanel::updateBookmarkButton);
        GlobalLogger.log("Object Explorer - Unbookmarked " + entry + (!wasInList ? " (Element was not in list)" : ""));
    }

    private void initModel() {
        setModel(new DefaultTableModel(0, 3) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(false);
        setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setSortsOnUpdates(true);
    }

    private void initRenderers() {
        DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (getRowValue(row).equals(currentDump)) {
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                    comp.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusAlertYellow));
                } else {
                    comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
                    if (isSelected) {
                        comp.setForeground(new Color(200, 0, 0));
                    } else {
                        comp.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UIText));
                    }
                }
                return comp;
            }

        };
        getColumnModel().getColumn(0).setCellRenderer(rend);
        getColumnModel().getColumn(1).setCellRenderer(rend);
        getColumnModel().getColumn(2).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JToggleButton but = new JToggleButton("Delete");
                but.setSelected(hasFocus);
                return but;
            }
        });
    }

    void updateBookmarkBrowser() {
        String[] bookmarks = Options.INSTANCE.getOEBookmarks(this.dm.getPatchType());
        // This mouse listener handles all of our object dumping / bookmark removing.
        clearSelection();
        final String[] header = new String[]{"Class", "Object", ""};
        String[][] data = new String[bookmarks.length][2];
        for (int i = 0; i < bookmarks.length; i++) {
            int idx = bookmarks[i].indexOf("'");
            if (idx != -1) {
                data[i][0] = bookmarks[i].substring(0, idx);
                data[i][1] = bookmarks[i].substring(idx + 1, bookmarks[i].indexOf("'", idx + 1));
            } else if (bookmarks[i].contains(".")) {
                data[i][1] = bookmarks[i];
                Dump dump = this.dm.getDump(bookmarks[i]);
                if (dump.ueObject != null && dump.ueObject.getUeClass() != null) {
                    data[i][0] = dump.ueObject.getUeClass().getName();
                } else {
                    data[i][0] = "";
                }
            } else {
                data[i][0] = bookmarks[i];
            }
        }

        ((DefaultTableModel) getModel()).setDataVector(data, header);
        updateColumnWidths();
        initRenderers();//setDataVector resets these too

        repaint();
    }

    private void updateColumnWidths() {
        for (int i = 0; i < 2; i++) {
            int width = 0;
            for (int j = 0; j < getRowCount(); j++) {
                width = Math.max(width, getFontMetrics(getFont()).stringWidth((String) getValueAt(j, i)));
            }
            getColumnModel().getColumn(i).setMinWidth(width + 10);
        }
        if (getParent() != null) {//This is where we implement our "fill the horizontal space we have available" scheme
            JScrollPane parent = (JScrollPane) getParent().getParent();//first parent is the viewport
            int availableWidth = parent.getPreferredSize().width;
            if (parent.getVerticalScrollBar().isVisible()) {
                availableWidth -= parent.getVerticalScrollBar().getPreferredSize().width;
            }
            availableWidth -= getColumnCount() + 1;//The four pixels that seperate the columns / are on the edges
            availableWidth -= 2;//no clue
            int otherColums = getColumnModel().getColumn(0).getMinWidth() + getColumnModel().getColumn(2).getPreferredWidth();
            getColumnModel().getColumn(1).setMinWidth(Math.max(getColumnModel().getColumn(1).getMinWidth(), availableWidth - otherColums));
        }
    }

}
