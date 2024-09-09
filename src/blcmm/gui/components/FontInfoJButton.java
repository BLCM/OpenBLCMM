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
import java.awt.Font;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolTip;

/**
 * An extension to JButton which supports scaling the tooltip font based on
 * the user's font-size selection.  (Only really needed for sessions in which
 * the user's changing the size dynamically.)
 *
 * This class also includes a single constructor which allows setting a font
 * style + size modifier which should be applied whenever the label's font
 * size changes.
 *
 * @author apocalyptech
 */
public class FontInfoJButton extends JButton {

    private final FontInfo fontInfo;
    private boolean doExtraStyle = false;
    private int extraStyle = Font.PLAIN;
    private float extraFontSize = 0;

    public FontInfoJButton(FontInfo fontInfo) {
        super();
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJButton(FontInfo fontInfo, int extraStyle, float extraFontSize) {
        super();
        this.fontInfo = fontInfo;
        this.doExtraStyle = true;
        this.extraStyle = extraStyle;
        this.extraFontSize = extraFontSize;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJButton(Action a, FontInfo fontInfo) {
        super(a);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJButton(Icon icon, FontInfo fontInfo) {
        super(icon);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJButton(String text, FontInfo fontInfo) {
        super(text);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJButton(String text, Icon icon, FontInfo fontInfo) {
        super(text, icon);
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

    @Override
    public void setFont(Font f) {
        if (doExtraStyle) {
            super.setFont(f.deriveFont(this.extraStyle, f.getSize2D() + this.extraFontSize));
        } else {
            super.setFont(f.deriveFont(f.getSize2D() + this.extraFontSize));
        }
    }

}
