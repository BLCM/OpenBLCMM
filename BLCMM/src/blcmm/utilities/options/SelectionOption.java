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
package blcmm.utilities.options;

import blcmm.gui.panels.ToolSettingsPanel;
import blcmm.utilities.Options;
import blcmm.utilities.StringTable;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * A boolean option
 *
 * @author apocalyptech
 * @param <O>
 */
public class SelectionOption<O extends SelectionOptionData> extends Option<O> {

    private final O[] options;
    private final OptionDataConverter<O> converter;

    /**
     * Constructor for a boolean option. If displayDesc is null, the option will
     * not be shown on the settings panel.
     *
     * @param name Key for the option
     * @param defaultData Default value for the option
     * @param shownPanel The panel on which to show this option
     * @param displayDesc Display description on the settings panel
     * @param callback Callback to use when the option is changed
     * @param tooltip Tooltip to show on the control
     * @param options The possible options to choose from
     * @param converter a converter to go from String to the datatype.
     */
    public SelectionOption(String name,
            O defaultData,
            Option.Shown shownPanel,
            String displayDesc,
            String callback,
            String tooltip,
            O[] options,
            OptionDataConverter<O> converter) {
        this(name, defaultData, shownPanel, displayDesc, callback, tooltip, null, options, converter);
    }

    /**
     * Constructor for a boolean option. If displayDesc is null, the option will
     * not be shown on the settings panel.
     *
     * @param name Key for the option
     * @param defaultData Default value for the option
     * @param shownPanel The panel on which to show this option
     * @param displayDesc Display description on the settings panel
     * @param callback Callback to use when the option is changed
     * @param tooltip Tooltip to show on the control
     * @param setupCallback A callback to perform extra actions on component
     * setup
     * @param options The possible options to choose from
     * @param converter a converter to go from String to the datatype.
     */
    public SelectionOption(String name,
            O defaultData,
            Option.Shown shownPanel,
            String displayDesc,
            String callback,
            String tooltip,
            String setupCallback,
            O[] options,
            OptionDataConverter<O> converter) {
        super(name, defaultData, shownPanel, displayDesc, callback, tooltip, setupCallback);
        this.options = options;
        this.converter = converter;
    }

    /**
     * Converts the given string (read from a config file) into the correct data
     * type for this Option.
     *
     * @param stringData The string data to convert
     * @return The data in its proper format
     */
    @Override
    public O stringToData(String stringData) {
        return converter.convert(stringData);
    }

    /**
     * Converts the current data for this option into a String suitable for
     * saving to a text-based options file.
     *
     * @return A string representation of our data
     */
    @Override
    public String dataToString() {
        return getData().toSaveString();
    }

    /**
     * Return a JComponent for this option, for use in the settings panel.
     *
     * @param panel The ToolSettingsPanel object we are being added to
     * @return A suitable JComponent
     */
    @Override
    public JComponent getGUIComponent(ToolSettingsPanel panel) {
        SelectionOption option = this;
        JComboBox<O> box = new JComboBox<>(options);
        box.setSelectedItem(this.getData());
        box.addItemListener(ie -> {
            setData((O) box.getSelectedItem());
            panel.callBack(option, box);
            Options.INSTANCE.save();
        });
        return box;
    }

    public static SelectionOption createStringSelectionOption(String name,
            String defaultData,
            Option.Shown shownPanel,
            String displayDesc,
            String callback,
            String tooltip,
            StringTable table) {
        StringSelectionOptionDataConverter conv = new StringSelectionOptionDataConverter(defaultData, Arrays.asList(table.keySet().toArray(new String[0])));
        SelectionOption res = new SelectionOption(name, conv.def, shownPanel, displayDesc, callback, tooltip, conv.options.toArray(new StringSelectionOptionData[0]), conv) {
            @Override
            public JComponent getGUIComponent(ToolSettingsPanel panel) {
                JComboBox guiComponent = (JComboBox) super.getGUIComponent(panel);
                guiComponent.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> jlist, Object o, int i, boolean bln, boolean bln1) {
                        JLabel l = (JLabel) super.getListCellRendererComponent(jlist, o, i, bln, bln1);
                        l.setToolTipText(table.get(o.toString(), "tooltip"));
                        return l;
                    }
                });
                return guiComponent;
            }

        };
        return res;

    }

    private static class StringSelectionOptionData implements SelectionOptionData {

        private final String value;

        StringSelectionOptionData(String value) {
            this.value = value;
        }

        @Override
        public String toSaveString() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }

    }

    private static class StringSelectionOptionDataConverter implements OptionDataConverter<StringSelectionOptionData> {

        private final StringSelectionOptionData def;
        private final Collection<StringSelectionOptionData> options;

        StringSelectionOptionDataConverter(String def, Collection<String> options) {
            this.def = new StringSelectionOptionData(def);
            this.options = new HashSet<>();
            for (String s : options) {
                this.options.add(new StringSelectionOptionData(s));
            }
        }

        @Override
        public StringSelectionOptionData convert(String s) {
            for (StringSelectionOptionData option : options) {
                if (option.value.equals(s)) {
                    return option;
                }
            }
            return def;
        }

    }

}
