/*
 * Copyright (C) 2018-2020  LightChaosman
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
package blcmm.utilities.options;

import blcmm.gui.FontInfo;
import blcmm.gui.panels.ToolSettingsPanel;
import blcmm.utilities.OptionsBase;
import javax.swing.JComponent;

/**
 * Base class for options that we'll set in our setting menu.
 *
 * @param <T> Basic data type for option
 *
 * @author apocalyptech
 */
public abstract class Option<T> {

    // Arguably this should be in blcmm.utilities.Options instead
    public enum Shown {
        NONE,
        SETTINGS,
        INPUT,
        OE,
        DANGEROUS
    }

    private final String name;

    private final boolean onlyVisual;

    private T data;

    private T defaultData;

    private boolean setData;

    protected Shown shownPanel;

    private final String displayDesc;

    private final String callback;

    private final String tooltip;

    private final String setupCallback;

    protected final OptionsBase optionsObj;

    protected final FontInfo fontInfo;

    /**
     * Constructor for an option which won't be displayed on the settings panel.
     *
     * @param optionsObj The Options that this Option is a part of
     * @param name Key of the option
     * @param fontInfo Font information to use on the option
     * @param defaultData Default data for the option
     */
    public Option(OptionsBase optionsObj, String name, FontInfo fontInfo, T defaultData) {
        this(optionsObj, name, fontInfo, defaultData, Shown.NONE, false, null, null, null, null);
    }

    /**
     * Constructor for an "option" which is actually only a visual UI element,
     * and not an actual real option.
     *
     * @param optionsObj The Options that this Option is a part of
     * @param name The name of the option
     * @param fontInfo Font information to use on the option
     * @param shownPanel The panel on which we'll be shown.
     */
    public Option(OptionsBase optionsObj, String name, FontInfo fontInfo, Shown shownPanel) {
        this(optionsObj, name, fontInfo, null, shownPanel, true, null, null, null, null);
    }

    /**
     * Constructor for an option. If displayDesc is null, the option will not be
     * shown on the settings panel.
     *
     * @param optionsObj The Options that this Option is a part of
     * @param name Key of the option
     * @param fontInfo Font information to use on the option
     * @param defaultData Default data for the option
     * @param shownPanel The panel on which to show this option
     * @param displayDesc Display description on the settings panel.
     * @param callback A callback function to call when the option changes. Pass
     * null to not have a callback.
     * @param tooltip Tooltip to show on the control
     */
    public Option(OptionsBase optionsObj,
            String name,
            FontInfo fontInfo,
            T defaultData,
            Shown shownPanel,
            String displayDesc,
            String callback, String tooltip) {
        this(optionsObj, name, fontInfo, defaultData, shownPanel, false, displayDesc, callback, tooltip, null);
    }

    /**
     * Constructor for an option. If displayDesc is null, the option will not be
     * shown on the settings panel.
     *
     * @param optionsObj The Options that this Option is a part of
     * @param name Key of the option
     * @param fontInfo Font information to use on the option
     * @param defaultData Default data for the option
     * @param shownPanel The panel on which to show this option
     * @param displayDesc Display description on the settings panel.
     * @param callback A callback function to call when the option changes. Pass
     * null to not have a callback.
     * @param tooltip Tooltip to show on the control
     * @param setupCallback A callback function to call while setting up the
     * option. Pass null to let it be set up as usual.
     */
    public Option(OptionsBase optionsObj,
            String name,
            FontInfo fontInfo,
            T defaultData,
            Shown shownPanel,
            String displayDesc,
            String callback, String tooltip,
            String setupCallback) {
        this(optionsObj, name, fontInfo, defaultData, shownPanel, false, displayDesc, callback, tooltip, setupCallback);
    }

    /**
     * Constructor for an option. If displayDesc is null, the option will not be
     * shown on the settings panel.
     *
     * @param options The Options that this Option is a part of
     * @param name Key of the option
     * @param fontInfo Font information to use on the option
     * @param defaultData Default data for the option
     * @param shownPanel The panel on which to show this option
     * @param onlyVisual True if this element is purely visual, or false if it's
     * a regular option.
     * @param displayDesc Display description on the settings panel.
     * @param callback A callback function to call when the option changes. Pass
     * null to not have a callback.
     * @param tooltip Tooltip to show on the control
     * @param setupCallback A callback function to call while setting up the
     * option. Pass null to let it be set up as usual.
     */
    public Option(OptionsBase options,
            String name,
            FontInfo fontInfo,
            T defaultData,
            Shown shownPanel, boolean onlyVisual,
            String displayDesc,
            String callback, String tooltip,
            String setupCallback) {
        this.optionsObj = options;
        this.name = name;
        this.fontInfo = fontInfo;
        this.defaultData = defaultData;
        this.shownPanel = shownPanel;
        this.onlyVisual = onlyVisual;
        this.displayDesc = displayDesc;
        this.data = defaultData;
        this.setData = false;
        this.callback = callback;
        this.tooltip = tooltip;
        this.setupCallback = setupCallback;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if this "option" is a visual UI element only, or false if
     * it's a real option.
     *
     * @return True if only visual, false if real option
     */
    public boolean isOnlyVisual() {
        return this.onlyVisual;
    }

    /**
     * @param shownPanel The panel we're rendering
     * @return Whether to show the option on this Panel
     */
    public boolean isDisplayOnPanel(Shown shownPanel) {
        return (this.shownPanel == shownPanel && (this.onlyVisual || this.displayDesc != null));
    }

    /**
     * @return the displayDesc
     */
    public String getDisplayDesc() {
        return displayDesc;
    }

    /**
     * @return the callback
     */
    public String getCallback() {
        return callback;
    }

    /**
     * @return the tooltip
     */
    public String getTooltip() {
        return tooltip;
    }

    /**
     * @return the setup callback
     */
    public String getSetupCallback() {
        return setupCallback;
    }

    /**
     * Get the data for this setting.
     *
     * @return The data
     */
    public T getData() {
        return this.data;
    }

    /**
     * Sets the data for this option.
     *
     * @param newData The new data to set
     */
    public void setData(T newData) {
        this.data = newData;
        this.setData = true;
    }

    /**
     * Returns the default data for this option.
     *
     * @return
     */
    public T getDefaultData() {
        return this.defaultData;
    }

    /**
     * Restore this option to its default value.
     */
    public void restoreDefault() {
        this.setData(this.defaultData);
        this.setData = false;
    }

    /**
     * Returns whether or not this option has been set (by something like
     * an options file or user choice).  Note that the value might still be
     * set to the default value, in the case that a user let the defaults
     * get saved out to the options file.
     *
     * @return True if our value has been set somehow, or false otherwise.
     */
    public boolean hasSetData() {
        return this.setData;
    }

    /**
     * Converts the given string (read from an options file) into the correct
     * data type for this Option.
     *
     * @param stringData The string data to convert
     * @return The data in its proper format
     */
    public abstract T stringToData(String stringData);

    /**
     * Converts the current data for this option into a String suitable for
     * saving to a text-based options file.
     *
     * @return A string representation of our data
     */
    public abstract String dataToString();

    /**
     * Return a JComponent for this option, for use in the settings panel.
     *
     * @param panel The ToolSettingsPanel object we are being added to
     * @return A suitable JComponent
     */
    public abstract JComponent getGUIComponent(ToolSettingsPanel panel);

    @Override
    public String toString() {
        return getClass() + ": " + dataToString();
    }

}
