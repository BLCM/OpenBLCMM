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
 */
package blcmm.gui.components;

import blcmm.gui.theme.ThemeManager;
import blcmm.gui.theme.ThemeManager.ColorType;
import general.utilities.GlobalLogger;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public class TimedLabel extends JLabel {

    private final long baseTime = 1000;
    private final int multiplier = 5;
    private boolean interupted = false;
    boolean reset = false;
    private final Map<String, StringIntTuple> storage = Collections.synchronizedMap(new LinkedHashMap<>());
    private Font originalFont;

    public TimedLabel() {
        super();
        new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                try {
                    String skip = null;
                    while (true) {
                        reset = false;
                        if (storage.isEmpty()) {
                            Thread.sleep(baseTime);
                        } else {
                            if (interupted) {
                                Thread.sleep(baseTime * multiplier);
                                interupted = false;
                            }
                            String[] keys = storage.keySet().toArray(new String[0]);
                            outer:
                            for (String key : keys) {
                                if (key.equals(skip)) {
                                    skip = null;
                                    continue;
                                }
                                if (!storage.get(key).canStillBeDisplayed()) {
                                    storage.remove(key);
                                    continue;
                                }
                                if (reset) {
                                    break;
                                }
                                storage.get(key).gonnaDisplay();
                                setLabel(key);

                                for (int i = 0; i < storage.get(key).weight * multiplier; i++) {
                                    if (reset) {
                                        break outer;
                                    }
                                    Thread.sleep(baseTime);
                                    if (interupted) {
                                        Thread.sleep(baseTime * multiplier);
                                        interupted = false;
                                        if (reset) {
                                            break outer;
                                        }
                                        setLabel(key);
                                        if (i < multiplier) {
                                            skip = key;
                                            continue outer;
                                        }
                                    }
                                    if (reset) {
                                        break outer;
                                    }
                                    setLabel(key);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    GlobalLogger.log(e);
                    throw e;
                }
            }

            private void setLabel(String key) {
                EventQueue.invokeLater(() -> {
                    TimedLabel.this.setTextPrivate(storage.get(key).string.toString());
                    TimedLabel.super.setForeground(storage.get(key).getColor());
                });
            }

        }.execute();
    }

    public void remove(String key) {
        storage.remove(key);
        reset = true;
    }

    public void putString(String key, String text, int weight) {
        putString(key, text, weight, ColorType.UIText);
    }

    public void putString(String key, String text, int weight, Color c) {
        putString_p(key, new StringIntTuple(weight, text, c));
    }

    public void putString(String key, String text, int weight, ColorType c) {
        putString_p(key, new StringIntTuple(weight, text, c));
    }

    public void putString(String key, DynamicString text, int weight) {
        putString(key, text, weight, ColorType.UIText);
    }

    public void putString(String key, DynamicString text, int weight, Color c) {
        putString_p(key, new StringIntTuple(weight, text, c));
    }

    public void putString(String key, DynamicString text, int weight, ColorType c) {
        putString_p(key, new StringIntTuple(weight, text, c));
    }

    public void putString(String key, String text, int weight, int maxDisplays) {
        putString(key, text, weight, ColorType.UIText, maxDisplays);
    }

    public void putString(String key, String text, int weight, Color c, int maxDisplays) {
        putString_p(key, new StringIntTuple(weight, text, c, maxDisplays));
    }

    public void putString(String key, String text, int weight, ColorType c, int maxDisplays) {
        putString_p(key, new StringIntTuple(weight, text, c, maxDisplays));
    }

    public void putString(String key, DynamicString text, int weight, int maxDisplays) {
        putString(key, text, weight, ColorType.UIText, maxDisplays);
    }

    public void putString(String key, DynamicString text, int weight, Color c, int maxDisplays) {
        putString_p(key, new StringIntTuple(weight, text, c, maxDisplays));
    }

    public void putString(String key, DynamicString text, int weight, ColorType c, int maxDisplays) {
        putString_p(key, new StringIntTuple(weight, text, c, maxDisplays));
    }

    private void putString_p(String key, StringIntTuple stringIntTuple) {
        storage.put(key, stringIntTuple);
    }

    public void showTemporary(String string) {
        showTemporary(string, ThemeManager.getColor(ThemeManager.ColorType.UIText));
    }

    public void showTemporary(String string, Color color) {
        setTextPrivate(string);
        super.setForeground(color);
        interupted = true;
    }

    @Override
    public void setFont(Font font) {
        originalFont = font;
        super.setFont(font);
    }

    @Override
    public void setText(String string) {
        showTemporary(string);
    }

    private void setTextPrivate(String string) {
        super.setFont(originalFont);
        setTextPrivate(string, 0, 2);
    }

    private void setTextPrivate(String string, int fontDecrease, final int maxDecrease) {
        Font f = getFont();
        if (f != null) {//Font is null during initialization
            int w = getSize().width;
            int w2 = 0;
            FontMetrics metrics = getFontMetrics(f);
            int i = 0;
            for (; i < string.length(); i++) {
                w2 += metrics.charWidth(string.charAt(i));
                if (w2 > w) {
                    i -= 3;
                    break;
                }
            }
            if (i < string.length() - 1 && i > 0 && string.length() > 3) {
                if (fontDecrease < maxDecrease) {
                    super.setFont(f.deriveFont(f.getSize2D() - 1f));
                    setTextPrivate(string, fontDecrease + 1, maxDecrease);
                    return;
                } else {
                    string = string.substring(0, i) + "...";
                }
            }
        }
        super.setText(string);
    }

    private final static class StringIntTuple {

        private final int weight;
        private final Object string;
        private final Color color;
        private final ColorType colorType;
        private int maxDisplays;

        StringIntTuple(int weight, String s, Color color) {
            this(weight, (Object) s, color, -1);
        }

        StringIntTuple(int weight, String string, Color color, int maxDisplays) {
            this(weight, (Object) string, color, maxDisplays);
        }

        StringIntTuple(int weight, DynamicString string, Color color) {
            this(weight, (Object) string, color, -1);
        }

        StringIntTuple(int weight, DynamicString string, Color color, int maxDisplays) {
            this(weight, (Object) string, color, maxDisplays);
        }

        StringIntTuple(int weight, String string, ColorType color) {
            this(weight, (Object) string, color, -1);
        }

        StringIntTuple(int weight, String string, ColorType color, int maxDisplays) {
            this(weight, (Object) string, color, maxDisplays);
        }

        StringIntTuple(int weight, DynamicString string, ColorType color) {
            this(weight, (Object) string, color, -1);
        }

        StringIntTuple(int weight, DynamicString string, ColorType c, int maxDisplays) {
            this(weight, (Object) string, c, maxDisplays);
        }

        private StringIntTuple(int weight, Object string, Color color, int maxDisplays) {
            this.weight = weight;
            this.string = string;
            this.color = color;
            this.colorType = null;
            this.maxDisplays = maxDisplays;
        }

        private StringIntTuple(int weight, Object string, ColorType color, int maxDisplays) {
            this.weight = weight;
            this.string = string;
            this.color = null;
            this.colorType = color;
            this.maxDisplays = maxDisplays;
        }

        public boolean canStillBeDisplayed() {
            return maxDisplays != 0;
        }

        public void gonnaDisplay() {
            assert maxDisplays != 0;
            maxDisplays--;
        }

        public Color getColor() {
            return color != null ? color : (colorType != null ? ThemeManager.getColor(colorType) : ThemeManager.getColor(ColorType.UIText));
        }
    }

    public static abstract class DynamicString {

        @Override
        public abstract String toString();
    }
}
