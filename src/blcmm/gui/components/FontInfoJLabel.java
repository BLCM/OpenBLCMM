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

import blcmm.gui.FontInfo;
import java.awt.Font;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

/**
 * An extension to JButton which supports scaling the tooltip font based on
 * the user's font-size selection.  (Only really needed for sessions in which
 * the user's changing the size dynamically.)
 *
 * This class also includes a single constructor which allows setting a font
 * size modifier which should be applied whenever the label's font size changes.
 *
 * @author apocalyptech
 */
public class FontInfoJLabel extends JLabel {

    private final FontInfo fontInfo;
    private float extraFontSize = 0;

    public FontInfoJLabel(FontInfo fontInfo) {
        super();
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJLabel(FontInfo fontInfo, float extraFontSize) {
        super();
        this.fontInfo = fontInfo;
        this.extraFontSize = extraFontSize;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJLabel(Icon image, FontInfo fontInfo) {
        super(image);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJLabel(Icon image, int horizontalAlignment, FontInfo fontInfo) {
        super(image, horizontalAlignment);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJLabel(String text, FontInfo fontInfo) {
        super(text);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJLabel(String text, Icon icon, int horizontalAlignment, FontInfo fontInfo) {
        super(text, icon, horizontalAlignment);
        this.fontInfo = fontInfo;
        // May as well do this too
        this.setFont(fontInfo.getFont());
    }

    public FontInfoJLabel(String text, int horizontalAlignment, FontInfo fontInfo) {
        super(text, horizontalAlignment);
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
        super.setFont(f.deriveFont(f.getSize2D() + this.extraFontSize));
    }
}
