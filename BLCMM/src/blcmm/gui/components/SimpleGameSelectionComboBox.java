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
package blcmm.gui.components;

import blcmm.model.PatchType;
import blcmm.utilities.Options;
import java.awt.Component;
import java.awt.event.ItemListener;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * A "Simple" game selection combo box, intended for use in OE.
 * Basically just copied from GameSelectionPanel with all the cruft trimmed
 * out.
 *
 * @author LightChaosman
 */
public class SimpleGameSelectionComboBox extends JComboBox {

    private PatchType type = null;

    public SimpleGameSelectionComboBox() {
        super(PatchType.values());
        this.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jlist, Object o, int i, boolean bln, boolean bln1) {
                JLabel l = (JLabel) super.getListCellRendererComponent(jlist, o, i, bln, bln1);
                l.setText(((PatchType) o).getGameName());
                l.setIcon(new ImageIcon(((PatchType) o).getIcon(4 + Options.INSTANCE.getFontsize())));
                return l;
            }
        });
        this.addItemListener(e -> type = (PatchType) this.getSelectedItem());
        this.setMode(type);
    }

    public final void setMode(PatchType type) {
        this.type = type;
        this.setSelectedItem(type);
    }

    public PatchType getNonNullGameType() {
        return type == null ? PatchType.BL2 : type;
    }

    public PatchType getGameType() {
        return type;
    }

    public void setType(PatchType type) {
        this.setMode(type);
    }

    public void addItemListenerToComboBox(ItemListener aListener) {
        this.addItemListener(aListener);
    }

}
