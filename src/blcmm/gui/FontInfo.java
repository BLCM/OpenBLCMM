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
package blcmm.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;

/**
 * This is a class to hold information about the font that the user has
 * selected (at the moment, just the font size), and some scaling information
 * compared to the default font which will be useful in constructing dialogs.
 *
 * Specifically, this class exists because we need to be able to launch dialogs
 * at varying sizes depending on the font size the user's selected.  We
 * accomplish this by storing the "default" font, and then comparing font
 * renders between that default and the currently-selected font, to find the
 * width/height scaling values between the fonts.  Then when constructing
 * dialogs in the app, we can get pretty good results by just scaling up our
 * "default" dialog sizes by the same scaling factors, to produce dialogs which
 * look pretty good regardless of the user's selected font size.
 *
 * The class also keeps track of a "line height" metric for the current font,
 * mostly just for blcmm.gui.componentsAdHocDialog, which uses it for some
 * extra calculation.
 *
 * If we ever decide to allow the user to change fonts in the app entirely,
 * this should probably still work quite well.
 *
 * @author apocalyptech
 */
public class FontInfo {

    private final Font defaultFont;
    private Font font;
    private double scaleWidth;
    private double scaleHeight;
    private int lineHeight;

    /**
     * Initialize a new FontInfo object, using the specified font as the
     * "default" font from which we'll compare everything.  Dialog sizes
     * throughout OpenBLCMM are generally based on "Dialog" in 12pt.
     *
     * @param defaultFont The font to consider "default" for the purposes of
     * scaling.
     */
    public FontInfo(Font defaultFont) {
        this.defaultFont = defaultFont;
        this.font = defaultFont;
        this.scaleWidth = 1f;
        this.scaleHeight = 1f;

        // This is *not* correct, but it's better than nothing.  In practice,
        // our Font gets set very early on in MainGUI initialization, so this'll
        // get set properly down in setFont().
        this.lineHeight = defaultFont.getSize();
    }

    /**
     * Returns a copy of this FontInfo object, in case a dialog or some other
     * element wants to hold on to its original font even if the app as a
     * whole gets updated.  Basically only ever intended to be used in the
     * settings menu, which is intentionally isolated from changing fonts
     * due to the GUI getting too weird without window size changes, and me
     * not wanting to deal with attempting to resize a "live" window.  This
     * is needed because of how the default-reset button works; it would
     * otherwise assume the new font size.
     *
     * @return A copy of this FontInfo object.
     */
    public FontInfo copy() {
        FontInfo newInfo = new FontInfo(this.defaultFont);
        newInfo.font = this.font;
        newInfo.scaleHeight = this.scaleHeight;
        newInfo.scaleWidth = this.scaleWidth;
        newInfo.lineHeight = this.lineHeight;
        return newInfo;
    }

    /**
     * Get the currently-selected user font.
     *
     * @return The user-selected font.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Sets our current font, and compute the various scaling parameters which
     * we intend to keep track of.  Requires passing in a Component in order
     * to do font rendering -- we need access to a Graphics object and some
     * FontMetrics objects.
     *
     * @param currentFont The new font to use throughout the app.
     * @param c The component we'll use to compute font rendering details.
     */
    public void setFont(Font currentFont, Component c) {
        // We *could* compare font sizes to save ourselves some work here, but
        // if we ever allow the user to choose their own font, too, then we'd
        // have to check for that too, and it doesn't seem worth the
        // complication.  So we're doing more work than we have to when the
        // user stays on the defualt font, but it hardly matters.
        this.font = currentFont;
        FontMetrics fmDefault = c.getFontMetrics(this.defaultFont);
        FontMetrics fmCurrent = c.getFontMetrics(this.font);
        Rectangle2D boundsDefault = fmDefault.getStringBounds(
                "The quick brown fox jumped over the lazy dog.",
                c.getGraphics());
        Rectangle2D boundsCurrent = fmCurrent.getStringBounds(
                "The quick brown fox jumped over the lazy dog.",
                c.getGraphics());
        this.scaleWidth = boundsCurrent.getWidth()/boundsDefault.getWidth();
        this.scaleHeight = boundsCurrent.getHeight()/boundsDefault.getHeight();
        this.lineHeight = (int)Math.ceil(boundsCurrent.getHeight());
    }

    /**
     * Gets the width scaling factor for the current font.
     *
     * @return The width scaling factor
     */
    public double getScaleWidth() {
        return this.scaleWidth;
    }
    /**
     * Gets the height scaling factor for the current font.
     *
     * @return The height scaling factor
     */

    public double getScaleHeight() {
        return this.scaleHeight;
    }

    /**
     * Gets the line height for the current font.  Note that this only takes
     * into account the font shape itself, *not* also any spacing inbetween
     * lines.
     *
     * @return The height of the font, in pixels
     */
    public int getLineHeight() {
        return lineHeight;
    }

}
