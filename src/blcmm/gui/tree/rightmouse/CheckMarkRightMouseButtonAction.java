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
 */
package blcmm.gui.tree.rightmouse;

import blcmm.gui.tree.CheckBoxTree;
import javax.swing.JCheckBoxMenuItem;

/**
 *
 * @author LightChaosman
 */
public abstract class CheckMarkRightMouseButtonAction extends RightMouseButtonAction {

    public CheckMarkRightMouseButtonAction(CheckBoxTree tree, String buttonName, boolean requiresUnlocked) {
        super(tree, new JCheckBoxMenuItem(buttonName), new Requirements(true, requiresUnlocked, false));
        super.getButton().addActionListener(e -> action());
    }

    public abstract void update();

}
