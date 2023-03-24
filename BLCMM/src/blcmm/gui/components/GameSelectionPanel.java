/*
 * Copyright (C) 2018-2020  LightChaosman
 *
 * BLCMM is free software: you can redistribute it and/or modify
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
 * combining it with BLCMM Launcher, BLCMM Lib Distributor, BLCMM
 * Resources, or BLCMM Utilities (or modified versions of those
 * libraries), containing parts covered by the terms of their
 * proprietary license, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 *
 */
package blcmm.gui.components;

import static blcmm.gui.MainGUI.INSTANCE;
import blcmm.model.PatchType;
import blcmm.utilities.Options;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author LightChaosman
 */
public class GameSelectionPanel extends JPanel {

    private final JLabel label = new JLabel();
    private final JComboBox<PatchType> box = new JComboBox<>(PatchType.values());
    private PatchType type = null;
    private boolean enabled = false;

    public GameSelectionPanel() {
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jlist, Object o, int i, boolean bln, boolean bln1) {
                JLabel l = (JLabel) super.getListCellRendererComponent(jlist, o, i, bln, bln1);
                l.setText(((PatchType) o).getGameName());
                l.setIcon(new ImageIcon(((PatchType) o).getIcon(4 + Options.INSTANCE.getFontsize())));
                return l;
            }
        });
        label.setHorizontalTextPosition(JLabel.LEFT);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!enabled && type != null) {
                    JOptionPane.showMessageDialog(INSTANCE,
                            "To change game types, you need to open a file which was made for that game.\n"
                            + "BLCMM shows here for which game your current file is, so you can't change it.\n"
                            + "After all, a mod that modifies varkid spawns doesn't really make sense in TPS, right?",
                            "Choose a different file to change games",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        box.addItemListener(e -> type = (PatchType) box.getSelectedItem());
        GameSelectionPanel.this.setMode(type, enabled);
    }

    public void setMode(PatchType type, boolean enabled) {
        this.enabled = enabled;
        this.type = type;
        this.box.setSelectedItem(type);

        this.removeAll();
        super.setLayout(new GridBagLayout());
        if (type == null) {
            label.setText("No file opened");
            label.setIcon(null);
            this.add(label, new GridBagConstraints(0, 0, 1, 1, 1d, 1d, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        } else {
            if (enabled) {
                label.setText("Game:");
                label.setIcon(null);
                this.add(label, new GridBagConstraints(0, 0, 1, 1, 1d, 1d, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                this.add(box, new GridBagConstraints(1, 0, 1, 1, 1d, 1d, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
            } else {
                label.setText("This is a file for " + type.toString());
                label.setIcon(new ImageIcon(type.getIcon(8 + Options.INSTANCE.getFontsize())));
                this.add(label, new GridBagConstraints(0, 0, 1, 1, 1d, 1d, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            }
        }
        this.add(Box.createHorizontalStrut(getMaxWidth()), new GridBagConstraints(0, 1, 2, 1, 1d, 1d, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        SwingUtilities.updateComponentTreeUI(this);
    }

    private int getMaxWidth() {
        JLabel temp = new JLabel();
        temp.setText("This is a file for " + PatchType.TPS.toString());
        temp.setIcon(new ImageIcon(PatchType.TPS.getIcon(8 + Options.INSTANCE.getFontsize())));
        return temp.getPreferredSize().width;
    }

    public PatchType getNonNullGameType() {
        return type == null ? PatchType.BL2 : type;
    }

    public PatchType getGameType() {
        return type;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.setMode(type, enabled);
    }

    public void setType(PatchType type) {
        this.setMode(type, enabled);
    }

    public void addItemListenerToComboBox(ItemListener aListener) {
        box.addItemListener(aListener);
    }

}
