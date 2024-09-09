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
 */
package blcmm.gui.panels;

import blcmm.gui.FontInfo;
import blcmm.gui.MainGUI;
import blcmm.gui.components.FontInfoJLabel;
import blcmm.gui.theme.Theme;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options;
import blcmm.utilities.options.Option;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A single tab in our settings panel.
 *
 * Note that we're passing in a Font to use as our base font -- I was unable
 * to find a reliable way of propagating a default font after the user has
 * changed the font size in the app, and eventually decided to just use a
 * sledgehammer instead.
 *
 * @author LightChaosman, apocalyptech
 */
public class ToolSettingsPanel extends JPanel {

    private final Option.Shown shownPanel;
    private final Options settings;
    private final Component focusAfterResetComponent;
    private boolean needsToolReset = false;
    private boolean needsTreeResize = false;
    private FontInfo fontInfo;
    private HashMap<Option, JComponent> componentCache;

    public ToolSettingsPanel(Option.Shown shownPanel, FontInfo fontInfo, Component focusAfterResetComponent) {
        this(shownPanel, fontInfo, focusAfterResetComponent, null);
    }

    public ToolSettingsPanel(Option.Shown shownPanel, FontInfo fontInfo, Component focusAfterResetComponent, String headerText) {
        this.shownPanel = shownPanel;
        this.focusAfterResetComponent = focusAfterResetComponent;
        this.settings = Options.INSTANCE;
        this.fontInfo = fontInfo;
        generateGUI(headerText);
    }

    private void generateGUI(String headerText) throws NumberFormatException {

        final ToolSettingsPanel inst = this;
        this.componentCache = new HashMap<> ();

        this.setLayout(new GridBagLayout());
        GridBagConstraints constr = new GridBagConstraints();
        final int BORDER = 5;
        final int SPACING = 20;
        constr.gridx = 0;
        constr.gridy = 0;
        constr.gridwidth = 1;
        constr.gridheight = 1;
        constr.weightx = 1;
        constr.weighty = 1;
        constr.insets = new Insets(0, 0, 0, 0);
        constr.anchor = GridBagConstraints.WEST;

        int y = 0;
        if (headerText != null) {
            JLabel headerLabel = new JLabel("<html><b>" + headerText + "</b></html>");
            headerLabel.setFont(this.fontInfo.getFont());
            constr.gridx = 0;
            constr.gridy = y;
            constr.gridwidth = 2;
            constr.fill = GridBagConstraints.NONE;
            constr.insets.right = BORDER;
            constr.insets.left = BORDER;
            constr.insets.top = y == 0 ? BORDER : 0;
            constr.insets.bottom = 5;
            constr.anchor = GridBagConstraints.CENTER;
            this.add(headerLabel, constr);
            y++;
        }

        constr.gridwidth = 1;
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.insets.bottom = 0;

        // Dummy button for doing some row-height measurements.  I'm not sure
        // if setting the font here actually accomplishes anything or not,
        // but we'll go ahead and try.
        JButton dummyButton = new JButton("dummy");
        dummyButton.setFont(this.fontInfo.getFont());

        for (Option o : settings.getDisplayedOptionList(this.shownPanel)) {
            constr.gridy = y;

            constr.gridx = 0;
            constr.weightx = 100;
            constr.insets.right = 0;
            constr.insets.left = BORDER;
            constr.insets.top = y == 0 ? BORDER : 0;

            JComponent comp = o.getGUIComponent(this);
            comp.setFont(this.fontInfo.getFont());

            if (o.isOnlyVisual()) {

                constr.gridwidth = 2;
                constr.insets.right = BORDER;
                constr.anchor = GridBagConstraints.CENTER;
                //constr.ipady = dummyButton.getMinimumSize().height - comp.getMinimumSize().height;//Makes every row the same height
                this.add(comp, constr);
                constr.gridwidth = 1;

            } else {

                this.componentCache.put(o, comp);

                constr.anchor = GridBagConstraints.WEST;
                FontInfoJLabel label1 = new FontInfoJLabel(o.getDisplayDesc(), this.fontInfo);
                this.add(label1, constr);

                constr.gridx = 1;
                constr.weightx = 1;
                constr.anchor = GridBagConstraints.EAST;
                constr.insets.right = BORDER;
                constr.insets.left = SPACING;
                constr.ipady = dummyButton.getMinimumSize().height - comp.getMinimumSize().height;//Makes every row the same height
                this.add(comp, constr);

                String tooltipString = o.getTooltip();
                if (tooltipString != null) {
                    label1.setToolTipText(tooltipString);
                    comp.setToolTipText(o.getTooltip());
                }

                // If we need to do something custom with the component setup, do so.
                this.tryCallBack(o.getSetupCallback(), o, comp);

            }

            y++;
        }

        constr.gridy = y + 1;
        constr.gridx = 0;
        constr.insets.right = SPACING;
        constr.insets.left = BORDER;
        constr.insets.bottom = BORDER;
        constr.ipady = 0;
        constr.anchor = GridBagConstraints.WEST;
        // At the moment, nothing actually requires a restart, so don't bother
        // showing this.  I'm keeping the label in place, though, just in case
        // it ever becomes necessary.
        //this.add(new JLabel("Some changes may require a restart"), constr);
        this.add(new JLabel(""), constr);

        constr.anchor = GridBagConstraints.EAST;
        constr.gridx = 1;
        constr.insets.right = BORDER;
        constr.insets.left = SPACING;
        JButton but = new JButton("Restore defaults");
        but.setFont(this.fontInfo.getFont());
        but.addActionListener(ae -> {
            Options.INSTANCE.restoreDefaults(this.shownPanel);
            for (Component c : inst.getComponents()) {
                inst.remove(c);
            }
            generateGUI(headerText);

            // Activate any callbacks we might have -- otherwise some of our
            // restored-to-default settings might not actually apply (like
            // font size, or theme, etc)
            for (Map.Entry<Option, JComponent> e : this.componentCache.entrySet()) {
                callBack(e.getKey(), e.getValue());
            }

            // So for *some* reason, on our "Dangerous settings" tab, if we don't
            // trigger a focus change like this, the GUI appears frozen up after
            // we hit the button.  The button sesms to remain clicked, mousing
            // over option checkboxes doesn't give any visual indication of mouseover,
            // and it generally just feels frozen.  If we mouseover the *tab*,
            // though,things recover.  So, we're now passing in the main JTabbedPane
            // when constructing these, and explicitly requesting focus after
            // hitting the Restore Defaults button.  This is *not* needed for the
            // first pane, but doing it doesn't hurt, so whatever.
            if (this.focusAfterResetComponent != null) {
                this.focusAfterResetComponent.requestFocus();
            }
        });
        this.add(but, constr);

        constr.gridx = 1;
        constr.gridy = y;
        constr.weighty = 1000;
        this.add(new JPanel(), constr);
    }

    public boolean needsToolReset() {
        return needsToolReset;
    }

    public boolean needsTreeResize() {
        return needsTreeResize;
    }

    /**
     * Given a methodName in this class, return the appropriate Method which can
     * then be called (or null, if the method cannot be found).
     *
     * @param methodName Name of the method to call
     * @return The Method object itself.
     */
    private Method getCallbackMethod(String methodName) {
        if (methodName == null || methodName.equals("")) {
            return null;
        }

        Class<?> c = this.getClass();
        Method[] allMethods = c.getDeclaredMethods();
        for (Method m : allMethods) {
            String mname = m.getName();
            if (mname.equalsIgnoreCase(methodName)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    /**
     * Call the given methodName, passing in Option option and JComponent
     * component.
     *
     * @param methodName The method to call
     * @param option The option related to the callback
     * @param component The component displaying the Option
     */
    private void tryCallBack(String methodName, Option option, JComponent component) {
        Method m = this.getCallbackMethod(methodName);
        if (m == null) {
            return;
        }
        try {
            m.invoke(this, option, component);
        } catch (InvocationTargetException | IllegalAccessException x) {
            x.printStackTrace();
            GlobalLogger.log("Unable to call callback: " + x.toString());
        }
    }

    /**
     * Called when the user changes one of the options in this panel.
     *
     * @param option The Option which was changed
     * @param component The Component which renders the Option.
     */
    public void callBack(Option option, JComponent component) {
        this.tryCallBack(option.getCallback(), option, component);
    }

    //Methods below are invoked trought the callBack() method, or via setup callbacks
    private void setTheme(Option option, JComponent component) {
        MainGUI.setTheme((Theme) ((JComboBox<Theme>) component).getSelectedItem());
        EventQueue.invokeLater(() -> {
            SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(ToolSettingsPanel.this));
            repaint();
        });
    }

    /**
     * Check to see if we're allowed to switch themes. Calls out to some MainGUI
     * methods to know whether or not this is allowed.
     *
     * @param option
     * @param comp
     */
    private void checkThemeSwitchAllowed(Option option, JComponent comp) {
        if (!MainGUI.INSTANCE.isThemeSwitchAllowed()) {
            comp.setEnabled(false);
            comp.setToolTipText(MainGUI.INSTANCE.getThemeSwitchDeniedTooltip());
        }
    }

    private void updateFontSizes(Option option, JComponent comp) {
        MainGUI.INSTANCE.updateFontSizes();
        repaint();
    }

    private void toggleTruncateCommands(Option option, JComponent component) {
        needsTreeResize = true;
    }

    private void toggleDeveloperMode(Option option, JComponent comp) {
        JCheckBox box = (JCheckBox) comp;
        MainGUI.INSTANCE.toggleDeveloperMode(box.isSelected());
    }

    private void updateMainGUITreeHighlights(Option option, JComponent component) {
        MainGUI.INSTANCE.updateComponentTreeUI();
    }

    private void updateOESearchCategories(Option option, JComponent component) {
        this.settings.updateOESearchCategories();
    }

    /**
     * Extra steps when toggling our check-for-new-versions checkbox.  This
     * is so that we can disable active version check notices on the main
     * GUI if the checkbox has been disabled.
     *
     * @param option The option being toggled
     * @param component The component which did the toggling
     */
    public void toggleCheckForNewVersions(Option option, JComponent component) {
        if (!(boolean)option.getData()) {
            MainGUI.INSTANCE.cancelVersionCheckNotices();
        }
    }

}
