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

import blcmm.gui.theme.ThemeManager;
import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.util.function.Function;
import javax.swing.JFormattedTextField;

/**
 *
 * @author LightChaosman
 * @param <T> The class we're comparing.
 */
public class EnhancedFormattedTextField<T> extends JFormattedTextField {

    private final Function<String, String> customValidator;
    private final Function<String, T> converter;

    public EnhancedFormattedTextField(Function<String, String> customValidator, Function<String, T> converter) {
        this.customValidator = customValidator;
        this.converter = converter;
        super.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateTooltip();
            }
        });
        super.setFormatterFactory(new AbstractFormatterFactory() {
            @Override
            public AbstractFormatter getFormatter(JFormattedTextField xxxx) {
                final EnhancedFormattedTextField<T> tf = (EnhancedFormattedTextField<T>) xxxx;
                AbstractFormatter formatter = new AbstractFormatter() {
                    @Override
                    public T stringToValue(String text) throws ParseException {
                        if (tf.customValidator.apply(text) != null) {
                            throw new ParseException(tf.customValidator.apply(text), 0);
                        } else {
                            try {
                                T t = tf.converter.apply(text);
                                return t;
                            } catch (Exception e) {
                                throw new ParseException(e.getMessage(), 0);
                            }
                        }
                    }

                    @Override
                    public String valueToString(Object value) throws ParseException {
                        if (value == null) {
                            return "";
                        }
                        return value.toString();
                    }
                };
                return formatter;
            }
        });
        super.setFocusLostBehavior(COMMIT);
    }

    public void updateTooltip() {
        String tooltip = customValidator.apply(getText());
        setToolTipText(tooltip);
        if (tooltip == null) {
            setForeground(ThemeManager.getColor(ThemeManager.ColorType.UIText));
            setBackground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusLightBackground));
        } else {
            setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusRed));
            Color c1 = ThemeManager.getColor(ThemeManager.ColorType.UINimbusRed);
            Color c2 = ThemeManager.getColor(ThemeManager.ColorType.UIText);
            Color c3 = new Color((c1.getRed() + c2.getRed()) / 2, (c1.getGreen() + c2.getGreen()) / 2, (c1.getBlue() + c2.getBlue()) / 2);
            setBackground(c3);
        }
        revalidate();
    }

    public static Function<String, String> getIntegerValidator(int min, int max) {
        return s -> {
            s = s.trim();
            if (s.isEmpty()) {
                return null;
            }
            for (int i = 0; i < s.length(); i++) {
                if (!Character.isDigit(s.charAt(i))) {
                    if (!(i == 0 && s.charAt(i) == '-')) {
                        return "Please insert a number";
                    }
                }
            }
            try {
                int n = Integer.parseInt(s);
                if (n < min) {
                    return "Please insert a number at least " + min;
                }
                if (n > max) {
                    return "Please insert a number at most " + max;
                }
            } catch (NumberFormatException e) {
                return "Please insert a valid 32 bit signed integer, between -2^32 and 2^32-1";
            }
            return null;
        };
    }

    @Override
    public void setValue(Object value) {
        super.setValue(value);
        updateTooltip();
    }

    @Override
    public T getValue() {
        return (T) super.getValue(); //To change body of generated methods, choose Tools | Templates.
    }

}
