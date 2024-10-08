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
package blcmm.gui.components;

import blcmm.gui.FontInfo;
import blcmm.gui.theme.ThemeManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextField;
import javax.swing.JToolTip;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public class DefaultTextTextField extends JTextField {

    private final String hint;
    private boolean showingHint;
    private final FontInfo fontInfo;

    public DefaultTextTextField(final String hint, FontInfo fontInfo) {
        super(hint);
        this.hint = hint;
        this.showingHint = true;
        this.fontInfo = fontInfo;
        super.addFocusListener(new MyFocusListener());
        super.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusDisabledText));
    }

    @Override
    public String getText() {
        return showingHint ? "" : super.getText();
    }

    @Override
    public JToolTip createToolTip() {
        JToolTip tip = new JToolTip();
        tip.setComponent(this);
        tip.setFont(this.fontInfo.getFont());
        return tip;
    }

    private class MyFocusListener implements FocusListener {

        @Override
        public void focusGained(FocusEvent e) {
            if (getText().isEmpty()) {
                setText("");
                setForeground(ThemeManager.getColor(ThemeManager.ColorType.UIText));
                showingHint = false;
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (getText().isEmpty()) {
                setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusDisabledText));
                setText(hint);
                showingHint = true;

            }
        }
    }
}
