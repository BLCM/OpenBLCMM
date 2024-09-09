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

import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * An attempt to be a somewhat generic Progress-bar based dialog which can be
 * shown to the user when a long-running task involving a known number of items
 * is involved.
 *
 * There are three widgets shown on the dialog: 1) A status counter showing the
 * currently-processing item (out of N) 2) An extra text line, supplied by
 * whatever's using this dialog. 3) A progress bar
 */
public class ProgressDialog extends JDialog {

    private final int totalCount;
    private int curCount = 0;
    private final String displayFormatString;
    private final JLabel progressText;
    private final JLabel extraText;
    private final JProgressBar progressBar;

    /**
     * Initializes a new modal dialog.
     *
     * @param parent The parent Frame we've been spawned from
     * @param totalCount Total number of items being processed
     * @param titleFormatString A string to format as the title of the dialog.
     * Requires two format strings, both %s - the first will be the total number
     * of items, the second will be used to pluralize the title.
     * @param titleSingularSuffix The suffix to use when the title is singular
     * @param titlePluralSuffix The suffix to use when the title is plural
     * @param displayFormatString A string to format to show the numerical
     * status of the progress. Requires two format strings, both %d - the first
     * will be the current count, the second will be the total count.
     */
    public ProgressDialog(Frame parent, int totalCount,
            String titleFormatString,
            String titleSingularSuffix, String titlePluralSuffix,
            String displayFormatString) {
        super(parent, Dialog.ModalityType.APPLICATION_MODAL);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.totalCount = totalCount;
        this.displayFormatString = "<html><b>" + displayFormatString + "</b>";
        this.setTitle(String.format(titleFormatString,
                totalCount,
                (totalCount == 1 ? titleSingularSuffix : titlePluralSuffix))
        );

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;

        // Add the progress label
        this.progressText = new JLabel();
        panel.add(this.progressText, c);
        c.gridy++;

        // Extra text to show to the user
        this.extraText = new JLabel();
        panel.add(this.extraText, c);
        c.gridy++;

        // Now the actual progress bar
        c.fill = GridBagConstraints.HORIZONTAL;
        this.progressBar = new JProgressBar(0, this.totalCount);
        panel.add(this.progressBar, c);
        c.gridy++;

        this.add(panel);
        this.setPreferredSize(new Dimension(400, 150));
        this.setResizable(false);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        this.pack();
        this.setLocationRelativeTo(parent);

        this.updateProgress(0, "");

    }

    /**
     * Return the total count that we're going towards
     *
     * @return The count
     */
    public int getTotalCount() {
        return this.totalCount;
    }

    /**
     * Update our current progress with a specific number.
     *
     * @param current The number we're currently on
     * @param extraText Extra text to display beneath the counter
     */
    public void updateProgress(int current, String extraText) {
        this.curCount = current;
        this.updateProgressText(extraText);
    }

    /**
     * Increments the current process by one (dialog starts at zero).
     *
     * @param extraText Extra text to display beneath the counter
     */
    public void incrementProgress(String extraText) {
        this.curCount++;
        this.updateProgressText(extraText);
    }

    /**
     * Updates our display using the given extra text. Will also set our
     * progress counters, both textual and progress bar.
     *
     * @param extraText Extra text to display beneath the counter
     */
    public void updateProgressText(String extraText) {
        this.progressText.setText(String.format(
                this.displayFormatString, this.curCount, this.totalCount
        ));
        this.extraText.setText(extraText);
        this.progressBar.setValue(this.curCount > 0 ? this.curCount - 1 : 0);
    }

}
