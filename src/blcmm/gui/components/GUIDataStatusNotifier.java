/*
 * Copyright (C) 2023 Christopher J. Kucera
 * <cj@apocalyptech.com>
 * <https://apocalyptech.com/contact.php>
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

import blcmm.Meta;
import blcmm.data.lib.DataStatusNotifier;
import blcmm.gui.FontInfo;
import blcmm.gui.MainGUI;
import blcmm.model.PatchType;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Utilities;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * GUI Status updates for our data extraction/verification progress.  This will
 * only start showing statuses if a "major" event is seen.  Once we're showing
 * messages, we'll show every one, whether it's major or not.  We'll also
 * display the entire event list, even if there were minor events prior to the
 * first major one.
 *
 * @author apocalyptech
 */
public class GUIDataStatusNotifier implements DataStatusNotifier {

    private PatchType curGame;
    private boolean showingGui;
    private JDialog dialog;
    private JScrollPane scroller;
    private final JTextPane text;
    private final HTMLDocument doc;
    private JButton okButton;
    private FontInfo fontInfo;

    public GUIDataStatusNotifier(FontInfo fontInfo) {
        this.fontInfo = fontInfo;
        this.curGame = null;
        this.showingGui = false;
        this.text = new JTextPane();
        this.text.setContentType("text/html");
        this.text.setEditorKit(new HTMLEditorKit());
        this.text.setText("<html>");
        this.text.setEditable(false);
        this.text.setOpaque(false);
        this.doc = (HTMLDocument) this.text.getStyledDocument();
    }

    /**
     * Appends the given HTML message to our main message area.
     *
     * @param message The message to append
     */
    private void append(String message) {
        try {
            this.doc.insertAfterEnd(this.doc.getCharacterElement(this.doc.getLength()), message + "<br>\n");
        } catch (BadLocationException|IOException e) {
            GlobalLogger.log("Error updating GUI Data Status Text!");
            GlobalLogger.log(e);
        }
        if (this.showingGui) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum());
                }
            });
        }
    }

    /**
     * Sets the Game whose data is currently being processed.
     *
     * @param game The game being processed.
     */
    @Override
    public void setGame(PatchType game) {
        if (this.curGame != game) {
            if (this.curGame != null) {
                this.append("");
            }
            this.append("<b>Processing " + game.getGameName() + " Data</b>");
        }
        this.curGame = game;
    }

    /**
     * Respond to an event.  Events can be major or minor.  In this
     * implementation, we don't start displaying anything unless we see a
     * "major" event.
     *
     * @param message The message to display.
     * @param major True if the event is major, false if minor.
     */
    @Override
    public void event(String message, boolean major) {

        // First append the message to our label
        this.append(" - " + message);

        // Now, if it's a major event and we haven't already, start showing the GUI
        if (major && !this.showingGui) {
            this.showingGui = true;
            this.dialog = new JDialog(MainGUI.INSTANCE, "Game Data Processing | " + Meta.NAME + " version " + Meta.VERSION);
            this.dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());

            // Header label with general information
            JLabel header = new JLabel("Initializing Game Data");
            // I would ordinarily just use HTML labels for this, but I was having
            // a heck of a time getting GridBag to 1) center the text while also
            // 2) not putting each word on a new line, and the "<nobr>" tag fixed
            // up #2 for me, but the JVM used in Liberica NIK-compiled applications
            // apparently doesn't process that tag, so we were back to smooshed
            // text.  So, whatever -- use a regular text label and do some font
            // weirdness.  (Actually, since then, I got a response on the Liberica
            // NIK bugreport, and there's a workaround to get that stuff to work.
            // Still, I've already switched this over, so I'll just leave it.)
            Font font = header.getFont();
            header.setFont(font.deriveFont(Font.BOLD, (float)font.getSize()+6));
            panel.add(header, new GridBagConstraints(
                    // x, y
                    0, 0,
                    // width, height
                    3, 1,
                    // weights (x, y)
                    1, 1,
                    // anchor
                    GridBagConstraints.CENTER,
                    // fill
                    GridBagConstraints.NONE,
                    // insets
                    new Insets(5, 10, 10, 50),
                    // pad (x, y)
                    0, 0));

            // General Info label
            JLabel infoLabel = new JLabel("<html>Game data has been detected in the "
                    + Meta.NAME + " installation directory.  Hang tight while we verify "
                    + "its integrity and extract the database so we can make use of it!"
            );
            panel.add(infoLabel, new GridBagConstraints(
                    // x, y
                    0, 1,
                    // width, height
                    3, 1,
                    // weights (x, y)
                    1, 1,
                    // anchor
                    GridBagConstraints.NORTHWEST,
                    // fill
                    GridBagConstraints.HORIZONTAL,
                    // insets
                    new Insets(5, 10, 5, 10),
                    // pad (x, y)
                    0, 0));

            // Add in our main scroll area
            this.scroller = new JScrollPane(this.text);
            scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            panel.add(scroller, new GridBagConstraints(
                    // x, y
                    0, 2,
                    // width, height
                    3, 1,
                    // weights (x, y)
                    1, 500,
                    // anchor
                    GridBagConstraints.NORTH,
                    // fill
                    GridBagConstraints.BOTH,
                    // insets
                    new Insets(20, 10, 5, 10),
                    // pad (x, y)
                    0, 0));

            // Add an "OK" button (disabled until we're done)
            this.okButton = new JButton("OK");
            this.okButton.addActionListener(e -> this.dialog.dispose());
            this.okButton.setEnabled(false);
            panel.add(this.okButton, new GridBagConstraints(
                    // x, y
                    2, 3,
                    // width, height
                    1, 1,
                    // weights (x, y)
                    500, 1,
                    // anchor
                    GridBagConstraints.EAST,
                    // fill
                    GridBagConstraints.NONE,
                    // insets
                    new Insets(10, 10, 10, 10),
                    // pad (x, y)
                    0, 0));

            this.dialog.add(panel);
            this.dialog.pack();
            this.dialog.setSize(Utilities.scaleAndClampDialogSize(new Dimension(600, 400), fontInfo, this.dialog));
            this.dialog.setLocationRelativeTo(null);
            this.dialog.setVisible(true);
        }
    }

    /**
     * Used to indicate that data processing has finished.
     */
    @Override
    public void finish() {
        if (this.showingGui) {
            this.append("");
            this.append("<b>Done!</b>");
            this.okButton.setEnabled(true);
            this.dialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
}
