/*
 * Copyright (C) 2023 Christopher J. Kucera
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
package blcmm.gui.components;

import blcmm.gui.FontInfo;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;

/**
 * An extension to JToggleButton which supports scaling the tooltip font based on
 * the user's font-size selection.  (Only really needed for sessions in which
 * the user's changing the size dynamically.)
 *
 * @author apocalyptech
 */
public class FontInfoJToggleButton extends JToggleButton {

    private final FontInfo fontInfo;

    public FontInfoJToggleButton(FontInfo fontInfo) {
        super();
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJToggleButton(Action a, FontInfo fontInfo) {
        super(a);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJToggleButton(Icon icon, FontInfo fontInfo) {
        super(icon);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJToggleButton(Icon icon, boolean selected, FontInfo fontInfo) {
        super(icon, selected);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJToggleButton(String text, FontInfo fontInfo) {
        super(text);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJToggleButton(String text, boolean selected, FontInfo fontInfo) {
        super(text, selected);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJToggleButton(String text, Icon icon, FontInfo fontInfo) {
        super(text, icon);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJToggleButton(String text, Icon icon, boolean selected, FontInfo fontInfo) {
        super(text, icon, selected);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    @Override
    public JToolTip createToolTip() {
        JToolTip tip = new JToolTip();
        tip.setComponent(this);
        tip.setFont(this.fontInfo.getFont());
        return tip;
    }
}
