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
package blcmm.gui.panels;

import blcmm.gui.MainGUI;
import blcmm.gui.theme.Theme;
import blcmm.utilities.Options;
import blcmm.utilities.options.Option;
import general.utilities.GlobalLogger;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author LightChaosman
 */
public class ToolSettingsPanel extends JPanel {

    private final Options settings;
    private boolean needsToolReset = false;
    private boolean needsTreeResize = false;

    public ToolSettingsPanel() {
        settings = Options.INSTANCE;
        generateGUI();
    }

    private void generateGUI() throws NumberFormatException {

        final ToolSettingsPanel inst = this;

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
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.insets = new Insets(0, 0, 0, 0);
        constr.anchor = GridBagConstraints.WEST;

        int y = 0;
        for (Option o : settings.getDisplayedOptionList()) {
            constr.gridy = y;
            JLabel label1 = new JLabel(o.getDisplayDesc());

            constr.gridx = 0;
            constr.weightx = 100;
            constr.insets.right = 0;
            constr.insets.left = BORDER;
            constr.insets.top = y == 0 ? BORDER : 0;
            constr.anchor = GridBagConstraints.WEST;
            this.add(label1, constr);

            JComponent comp = o.getGUIComponent(this);

            constr.gridx = 1;
            constr.weightx = 1;
            constr.anchor = GridBagConstraints.EAST;
            constr.insets.right = BORDER;
            constr.insets.left = SPACING;
            constr.ipady = new JButton("dummy").getMinimumSize().height - comp.getMinimumSize().height;//Makes every row the same height
            this.add(comp, constr);

            String tooltipString = o.getTooltip();
            if (tooltipString != null) {
                label1.setToolTipText(tooltipString);
                comp.setToolTipText(o.getTooltip());
            }

            // If we need to do something custom with the component setup, do so.
            this.tryCallBack(o.getSetupCallback(), o, comp);

            y++;
        }

        constr.gridy = y + 1;
        constr.gridx = 0;
        constr.insets.right = SPACING;
        constr.insets.left = BORDER;
        constr.insets.bottom = BORDER;
        constr.ipady = 0;
        constr.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("Some changes may require a restart"), constr);

        constr.anchor = GridBagConstraints.EAST;
        constr.gridx = 1;
        constr.insets.right = BORDER;
        constr.insets.left = SPACING;
        JButton but = new JButton("Restore defaults");
        but.addActionListener(ae -> {
            Options.INSTANCE.restoreDefaults();
            for (Component c : inst.getComponents()) {
                inst.remove(c);
            }
            generateGUI();
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
        if (box.isSelected()) {
            int selection = JOptionPane.showConfirmDialog(this,
                    "Enabling this will allow you to insert and edit the actual lines of code.\n"
                    + "It will also give more insight and details in the mods you're using.\n"
                    + "Do not enable this feature if you do not know what you're doing.\n"
                    + "By enabling  this you confirm that you know at least the basics of modding.\n"
                    + "\n"
                    + "Proceed?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (selection != JOptionPane.YES_OPTION) {
                box.setSelected(false);
                Options.INSTANCE.setContentEdits(false);
            }
        }
        MainGUI.INSTANCE.setChangePatchTypeEnabled(box.isSelected());
        MainGUI.INSTANCE.updateComponentTreeUI();
    }

    private void toggleHighlightBVCErrors(Option option, JComponent component) {
        MainGUI.INSTANCE.updateComponentTreeUI();
    }
}
