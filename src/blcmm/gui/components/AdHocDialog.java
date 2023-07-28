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
import blcmm.utilities.Utilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

/**
 * This class is essentially my own custom alternative to JOptionPane.  It was
 * nice leaving all the details to JOptionPane, but there's a few OpenBLCMM-
 * specific requirements which were impossible to achieve without subclassing
 * JOptionPane, and the gymnastics required to get some of it to work even
 * with subclassing never really seemed worth it.
 *
 * This class does *not* intend to be a drop-in replacement for JOptionPane.
 * It doesn't actually share any method names, and argument ordering is totally
 * different.  In addition to the obvious syntax differences, here's the extra
 * stuff that AdHocDialog supports:
 *
 * 1. Setting a font size.  This will get applied to the buttons on the dialog,
 *    and also to the pane content, if that's a JLabel or String.  This is
 *    necessary in BLCMM because it's using the "Nimbus" Look-and-Feel,
 *    which just doesn't seem to be great at dynamically setting attributes
 *    like that.  Other L+Fs would probably respond better, but eh.
 *
 * 2. Automatically scaling the dialog size based on the user's font size
 *    preference.  Obviously that's pretty closely related to point #1.  This
 *    will be clamped to the resolution of the screen which will show the
 *    dialog.  Note that right now we're probably over-growing the height of
 *    dialogs when large font sizes are being used; will have to look into that.
 *
 * @author apocalyptech
 */
public class AdHocDialog {

    /**
     * Icons that we can show in the upper left hand corner
     */
    public enum IconType {
        NONE,
        QUESTION,
        INFORMATION,
        WARNING,
        ERROR;
    }

    /**
     * What buttons are available for the user to press
     */
    public enum Button {
        YES,
        NO,
        CANCEL,
        OK;
    }

    /**
     * What "sets" of buttons will we show on the dialog
     */
    public enum ButtonSet {
        YES_NO,
        OK,
        // It bothers me that No/Yes is the opposite order from Yes/No, but
        // that's what JOptionPane does, and I'd like to stay consistent with
        // the historical behavior.
        CANCEL_NO_YES,
    }

    private final Component parentComponent;
    private final Font font;
    private final Icon icon;
    private final Component message;
    private final JDialog dialog;
    private final JPanel buttonBar;
    private Button result = null;
    private boolean buttonAdded = false;

    // How much extra room does the dialog need, once we've computed the size
    // of the inner pane?  Width is essentially just hardcoded: 64 for the
    // icon, then an extra 40 for misc padding.
    private final int paneToDialogExtraWidth = 64+40;

    // For height, that'll technically depend on the line height, since it
    // depends on the buttons.  Will set that up below in the constructor.
    private final int paneToDialogExtraHeight;

    /**
     * Instantiates a new dialog, without an explicit size parameter.  This
     * will let Swing/AWT/Java/whatever determine the size of the dialog.
     *
     * @param parentComponent Our parent component which launched the dialog
     * @param fontInfo A FontInfo object describing the user's current font
     * selection
     * @param iconType The icon type to show in the dialog
     * @param title Title of the dialog
     * @param message The message to show in the dialog
     */
    public AdHocDialog(Component parentComponent,
            FontInfo fontInfo,
            IconType iconType,
            String title,
            Object message) {
        this(parentComponent,
                fontInfo,
                iconType,
                title,
                message,
                null);
    }

    /**
     * Instantiates a new ad-hoc dialog, with a specified proposed size.  The
     * proposed size is expected to be based on a 12-point "Dialog" font, and
     * will scale up appropriately to the user's selected font size.
     *
     * @param parentComponent Our parent component which launched the dialog
     * @param fontInfo A FontInfo object describing the user's current font
     * selection
     * @param iconType The icon type to show in the dialog
     * @param title Title of the dialog
     * @param message The message to show in the dialog
     * @param proposedDimension The proposed dimension for the dialog.  This
     * will get scaled according to the user's font selection, and clamped to
     * the availability
     */
    public AdHocDialog(Component parentComponent,
            FontInfo fontInfo,
            IconType iconType,
            String title,
            Object message,
            Dimension proposedDimension)
    {
        this.parentComponent = parentComponent;

        // We're basically just adding 2x the line height, which should cover
        // the JButtons at the bottom plus a bit more.
        this.paneToDialogExtraHeight = fontInfo.getLineHeight()*2;

        switch (iconType) {
            case NONE:
                this.icon = null;
                break;
            case QUESTION:
                this.icon = UIManager.getIcon("OptionPane.questionIcon");
                break;
            case INFORMATION:
                this.icon = UIManager.getIcon("OptionPane.informationIcon");
                break;
            case WARNING:
                this.icon = UIManager.getIcon("OptionPane.warningIcon");
                break;
            case ERROR:
            default:
                this.icon = UIManager.getIcon("OptionPane.errorIcon");
                break;
        }
        this.font = fontInfo.getFont();
        if (message instanceof JLabel) {
            // Honestly not sure if I *should* be applying this to JLabels...
            // Presumably if the user created a JLabel then they probably
            // have their own opinions as to what its content would look like.
            // Still, I'll leave it for now.
            ((JLabel)message).setFont(font);
            this.message = (JLabel)message;
        } else if (message instanceof String) {
            JLabel messageLabel = new JLabel((String)message);
            messageLabel.setFont(font);
            this.message = messageLabel;
        } else {
            this.message = (Component)message;
        }

        // Scale and clamp our dialog size, if we've been given a size
        Dimension dialogDimension = null;
        Dimension paneDimension = null;
        boolean haveSize = false;
        if (proposedDimension != null) {
            haveSize = true;
            dialogDimension = Utilities.scaleAndClampDialogSize(proposedDimension, fontInfo, parentComponent);
            paneDimension = new Dimension(
                    dialogDimension.width - this.paneToDialogExtraWidth,
                    dialogDimension.height - this.paneToDialogExtraHeight
            );
        }

        // Now start creating things.  First the outer dialog.
        Window window = Utilities.findWindow(parentComponent);
        if (window instanceof Frame) {
            this.dialog = new JDialog((Frame)window, title, true);
        } else {
            this.dialog = new JDialog((Dialog)window, title, true);
        }
        if (haveSize) {
            this.dialog.setMinimumSize(dialogDimension);
        }
        this.dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel dialogPanel = new JPanel();
        this.dialog.add(dialogPanel);

        // Layout for the dialog panel
        dialogPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // Icon
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        gc.weighty = 500;
        gc.anchor = GridBagConstraints.NORTH;
        gc.fill = GridBagConstraints.NONE;
        gc.ipadx = 25;
        gc.ipady = 25;
        if (this.icon != null) {
            dialogPanel.add(new JLabel(this.icon), gc);
        }

        // Contents
        JScrollPane contentScroll;
        if (message instanceof JScrollPane) {
            contentScroll = (JScrollPane)message;
        } else {
            contentScroll = new JScrollPane();
            contentScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            contentScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            ScrollablePanel contentPanel = new ScrollablePanel();
            contentPanel.setScrollableWidth(ScrollablePanel.ScrollableSizeHint.FIT);
            contentPanel.setScrollableHeight(ScrollablePanel.ScrollableSizeHint.STRETCH);
            contentPanel.setLayout(new GridBagLayout());
            contentPanel.add(this.message, new GridBagConstraints(
                    // x, y
                    0, 0,
                    // width, height
                    1, 1,
                    // weights (x, y)
                    1, 500,
                    // anchor
                    GridBagConstraints.NORTHWEST,
                    // fill
                    GridBagConstraints.HORIZONTAL,
                    // insets
                    new Insets(15, 0, 5, 10),
                    // pad (x, y)
                    0, 0
            ));
            contentScroll.setViewportView(contentPanel);
            contentScroll.setBorder(BorderFactory.createEmptyBorder());
        }
        if (haveSize) {
            contentScroll.setPreferredSize(paneDimension);
        }
        if (this.icon == null) {
            gc.gridx = 0;
            gc.gridwidth = 2;
            gc.insets = new Insets(10, 10, 10, 10);
        } else {
            gc.gridx = 1;
        }
        gc.weightx = 500;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.BOTH;
        gc.ipadx = 5;
        gc.ipady = 5;
        dialogPanel.add(contentScroll, gc);

        // Button bar
        this.buttonBar = new JPanel();
        this.buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.X_AXIS));
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 2;
        gc.weightx = 500;
        gc.weighty = 0;
        gc.anchor = GridBagConstraints.NORTHEAST;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(0, 0, 5, 10);
        dialogPanel.add(this.buttonBar, gc);

    }

    /**
     * Actually run the dialog, with the specified ButtonSet as options.  Will
     * return the Button which was pressed by the user.
     *
     * @param buttonSet The set of Buttons to show to the user
     * @return The Button that the user pressed
     */
    public Button runDialog(ButtonSet buttonSet) {
        switch (buttonSet) {
            case YES_NO:
                this.addButton(Button.YES);
                this.addButton(Button.NO);
                break;
            case CANCEL_NO_YES:
                this.addButton(Button.CANCEL);
                this.addButton(Button.NO);
                this.addButton(Button.YES);
                break;
            case OK:
            default:
                this.addButton(Button.OK);
                break;
        }
        this.dialog.pack();
        this.dialog.setLocationRelativeTo(this.parentComponent);
        this.dialog.setVisible(true);
        this.dialog.dispose();
        return this.result;
    }

    /**
     * Add the specified button to the button bar at the bottom of the dialog.
     *
     * @param buttonType The Button type to add.
     */
    private void addButton(Button buttonType) {
        if (this.buttonAdded) {
            this.buttonBar.add(Box.createHorizontalStrut(7));
        } else {
            this.buttonAdded = true;
        }
        String label;
        int mnemonic;
        switch (buttonType) {
            case YES:
                label = "Yes";
                mnemonic = java.awt.event.KeyEvent.VK_Y;
                break;
            case NO:
                label = "No";
                mnemonic = java.awt.event.KeyEvent.VK_N;
                break;
            case CANCEL:
                label = "Cancel";
                mnemonic = java.awt.event.KeyEvent.VK_C;

                // If there's a Cancel button, also allow the user to hit Esc
                // to close the dialog
                JRootPane rootPane = this.dialog.getRootPane();
                KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
                rootPane.registerKeyboardAction((ActionEvent ae) -> {
                        this.result = buttonType;
                        this.dialog.setVisible(false);
                    }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
                break;
            case OK:
            default:
                label = "OK";
                mnemonic = java.awt.event.KeyEvent.VK_O;
                // Could add something here, like we do for Cancel, so that
                // hitting Enter closes the dialog (since in general that'll
                // be the only button available, when there's "OK"), but I'm
                // not bothering at the moment because the OK button should
                // be auto-selected anyway, so hitting Enter already does the
                // trick unless the user does something to the dialog's focus.
                break;
        }
        JButton button = new JButton(label);
        button.setFont(this.font);
        button.setMnemonic(mnemonic);
        button.addActionListener((ActionEvent ae) -> {
            this.result = buttonType;
            this.dialog.setVisible(false);
        });
        this.buttonBar.add(button);
    }

    /**
     * Convenience function to launch a dialog while specifying all possible
     * parameters.  Will return the Button that the user hit.
     *
     * @param parentComponent Our parent component which launched the dialog
     * @param fontInfo A FontInfo object describing the user's current font
     * selection
     * @param iconType The icon type to show in the dialog
     * @param title Title of the dialog
     * @param message The message to show in the dialog
     * @param buttonSet The set of Buttons to show to the user
     * @param proposedDimension
     * @return The Button that the user pressed
     */
    public static Button run(Component parentComponent,
            FontInfo fontInfo,
            IconType iconType,
            String title,
            Object message,
            ButtonSet buttonSet,
            Dimension proposedDimension) {
        AdHocDialog ahd = new AdHocDialog(
                parentComponent,
                fontInfo,
                iconType,
                title,
                message,
                proposedDimension);
        return ahd.runDialog(buttonSet);
    }

    /**
     * Convenience function to launch a dialog with just a single default "OK"
     * button.  Will return the Button that the user hit (which will always
     * be Button.OK).
     *
     * @param parentComponent Our parent component which launched the dialog
     * @param fontInfo A FontInfo object describing the user's current font
     * selection
     * @param iconType The icon type to show in the dialog
     * @param title Title of the dialog
     * @param message The message to show in the dialog
     * @param proposedDimension The proposed dimension for the dialog.  This
     * will get scaled according to the user's font selection, and clamped to
     * the availability
     * @return The Button that the user pressed
     */
    public static Button run(Component parentComponent,
            FontInfo fontInfo,
            IconType iconType,
            String title,
            Object message,
            Dimension proposedDimension) {
        AdHocDialog ahd = new AdHocDialog(
                parentComponent,
                fontInfo,
                iconType,
                title,
                message,
                proposedDimension);
        return ahd.runDialog(ButtonSet.OK);
    }

    /**
     * Convenience function to launch a dialog without a specific size set.
     * Will return the Button that the user hit.
     *
     * @param parentComponent Our parent component which launched the dialog
     * @param fontInfo A FontInfo object describing the user's current font
     * selection
     * @param iconType The icon type to show in the dialog
     * @param title Title of the dialog
     * @param message The message to show in the dialog
     * @param buttonSet The set of Buttons to show to the user
     * @return The Button that the user pressed
     */
    public static Button run(Component parentComponent,
            FontInfo fontInfo,
            IconType iconType,
            String title,
            Object message,
            ButtonSet buttonSet) {
        AdHocDialog ahd = new AdHocDialog(
                parentComponent,
                fontInfo,
                iconType,
                title,
                message);
        return ahd.runDialog(buttonSet);
    }

    /**
     * Convenience function to launch a dialog without a specific size set,
     * and with just a single default "OK" button.  Will return the Button that
     * the user hit (which will always be Button.OK).
     *
     * @param parentComponent Our parent component which launched the dialog
     * @param fontInfo A FontInfo object describing the user's current font
     * selection
     * @param iconType The icon type to show in the dialog
     * @param title Title of the dialog
     * @param message The message to show in the dialog
     * @return The Button that the user pressed
     */
    public static Button run(Component parentComponent,
            FontInfo fontInfo,
            IconType iconType,
            String title,
            Object message) {
        AdHocDialog ahd = new AdHocDialog(
                parentComponent,
                fontInfo,
                iconType,
                title,
                message);
        return ahd.runDialog(ButtonSet.OK);
    }

}
