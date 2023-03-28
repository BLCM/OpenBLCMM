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
package blcmm.gui.theme;

import blcmm.gui.MainGUI;
import blcmm.gui.tree.CheckBoxTree;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.synth.SynthTreeUI;

/**
 * Contains all the coloration information for the application, for all
 * available themes.
 *
 * @author apocalyptech
 */
public class ThemeManager {

    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();

    /**
     * The default theme, if one has not been specified.
     */
    private static final Theme DEFAULT_THEME;

    /**
     * An Enum defining the various color types which we have in the app.
     */
    public enum ColorType {

        // The first bunch are for the UI in general
        UIControl,
        UIInfo,
        UINimbusBase,
        UINimbusAlertYellow,
        UINimbusDisabledText,
        UINimbusFocus,
        UINimbusGreen,
        UINimbusInfoBlue,
        UINimbusLightBackground,
        UINimbusOrange,
        UINimbusRed,
        UINimbusSelectedText,
        UINimbusSelectionBackground,
        UIText,
        UITreeIcons,
        UICancelButtonBackground,
        UITextLink,
        //
        // This next batch are for the tree highlighting.
        TreeMUTChecker,
        TreeCommentChecker,
        TreeSpecialCommentChecker,
        TreeHotfixChecker,
        TreeCompleteClassCommandChecker,
        TreeOverwriterChecker,
        TreePartialOverwriterChecker,
        TreeOverwrittenChecker,
        TreePartialOverwrittenChecker,
        TreeSyntaxError,
        TreeContentError,
        TreeStyleWarning,
        TreeRootNode,
        //
        // Now for code-editing colors (also Object Explorer)
        CodeText,
        CodeComment,
        CodeKeyword,
        CodeGDWord,
        CodeMTWord,
        CodeNumber,
        CodeDoubleQuote,
        CodeSingleQuote,
    }

    /**
     * The currently-selected theme
     */
    private static Theme curTheme;

    /**
     * Have we initialized the icon map?
     */
    private static boolean initializedTreeIconMap = false;

    // Define the colors for our themes.
    static {
        loadHardCodedThemes();
        Theme theme = getThemeOrCreateIfNeeded("Dark");
        DEFAULT_THEME = theme;
        curTheme = theme;
    }

    private static void loadHardCodedThemes() {
        loadDarkTheme();
        loadLightTheme();
    }

    private static void loadDarkTheme() {
        // Colors for the Dark theme.
        Theme theme = getThemeOrCreateIfNeeded("Dark");
        theme.put(ColorType.UIControl, new Color(64, 64, 64));
        theme.put(ColorType.UIInfo, new Color(128, 128, 128));
        theme.put(ColorType.UINimbusBase, new Color(0, 0, 0));
        theme.put(ColorType.UINimbusAlertYellow, new Color(248, 187, 0));
        theme.put(ColorType.UINimbusDisabledText, new Color(128, 128, 128));
        theme.put(ColorType.UINimbusFocus, new Color(115, 0, 0));
        theme.put(ColorType.UINimbusGreen, new Color(70, 128, 50));
        theme.put(ColorType.UINimbusInfoBlue, new Color(66, 139, 221));
        theme.put(ColorType.UINimbusLightBackground, new Color(27, 30, 33));
        theme.put(ColorType.UINimbusOrange, new Color(191, 98, 4));
        theme.put(ColorType.UINimbusRed, new Color(236, 46, 26));
        theme.put(ColorType.UINimbusSelectedText, new Color(200, 0, 0));
        theme.put(ColorType.UINimbusSelectionBackground, new Color(25, 0, 0));
        theme.put(ColorType.UIText, new Color(230, 230, 230));
        theme.put(ColorType.UITreeIcons, new Color(190, 190, 190));
        theme.put(ColorType.UICancelButtonBackground, new Color(10, 0, 0));
        theme.put(ColorType.UITextLink, new Color(170, 170, 255));
        theme.put(ColorType.TreeMUTChecker, new Color(192, 139, 200));
        theme.put(ColorType.TreeCommentChecker, theme.get(ColorType.UINimbusAlertYellow));
        theme.put(ColorType.TreeSpecialCommentChecker, theme.get(ColorType.UINimbusAlertYellow).brighter().brighter().brighter());
        theme.put(ColorType.TreeCompleteClassCommandChecker, theme.get(ColorType.UIText).darker());
        theme.put(ColorType.TreeHotfixChecker, theme.get(ColorType.UINimbusInfoBlue));
        theme.put(ColorType.TreeOverwriterChecker, ((Color) theme.get(ColorType.UINimbusGreen)).brighter());
        theme.put(ColorType.TreePartialOverwriterChecker, ((Color) theme.get(ColorType.UINimbusGreen)).brighter().brighter());
        theme.put(ColorType.TreeOverwrittenChecker, ((Color) theme.get(ColorType.UINimbusGreen)).darker());
        theme.put(ColorType.TreePartialOverwrittenChecker, ((Color) theme.get(ColorType.UINimbusGreen)).darker().darker());
        theme.put(ColorType.TreeSyntaxError, theme.get(ColorType.UINimbusRed));
        theme.put(ColorType.TreeContentError, theme.get(ColorType.UINimbusOrange));
        theme.put(ColorType.TreeStyleWarning, Color.MAGENTA);
        theme.put(ColorType.TreeRootNode, Color.CYAN);
        theme.put(ColorType.CodeText, theme.get(ColorType.UIText));
        theme.put(ColorType.CodeComment, new Color(0, 120, 0));
        theme.put(ColorType.CodeKeyword, theme.get(ColorType.UINimbusInfoBlue));
        theme.put(ColorType.CodeGDWord, theme.get(ColorType.UINimbusGreen));
        theme.put(ColorType.CodeMTWord, new Color(255, 0, 255));
        theme.put(ColorType.CodeNumber, theme.get(ColorType.UINimbusRed));
        theme.put(ColorType.CodeDoubleQuote, theme.get(ColorType.UINimbusOrange));
        theme.put(ColorType.CodeSingleQuote, theme.get(ColorType.UINimbusAlertYellow));
    }

    private static void loadLightTheme() {
        // Colors for the Light theme
        Theme theme = getThemeOrCreateIfNeeded("Light");
        theme.put(ColorType.UIControl, new Color(210, 210, 210));
        theme.put(ColorType.UIInfo, new Color(242, 242, 189));
        theme.put(ColorType.UINimbusBase, new Color(75, 75, 175));
        theme.put(ColorType.UINimbusAlertYellow, new Color(148, 150, 1));
        theme.put(ColorType.UINimbusDisabledText, new Color(142, 143, 145));
        theme.put(ColorType.UINimbusFocus, new Color(115, 164, 209));
        theme.put(ColorType.UINimbusGreen, new Color(30, 138, 10));
        theme.put(ColorType.UINimbusInfoBlue, new Color(47, 92, 180));
        theme.put(ColorType.UINimbusLightBackground, new Color(255, 255, 255));
        theme.put(ColorType.UINimbusOrange, new Color(220, 158, 9));
        theme.put(ColorType.UINimbusRed, new Color(237, 18, 18));
        theme.put(ColorType.UINimbusSelectedText, new Color(255, 80, 80));
        theme.put(ColorType.UINimbusSelectionBackground, new Color(157, 205, 238));
        theme.put(ColorType.UIText, new Color(0, 0, 0));
        theme.put(ColorType.UITreeIcons, new Color(100, 100, 100));
        theme.put(ColorType.UICancelButtonBackground, new Color(220, 110, 110));
        theme.put(ColorType.UITextLink, new Color(20, 20, 105));
        theme.put(ColorType.TreeMUTChecker, new Color(147, 9, 167));
        theme.put(ColorType.TreeCommentChecker, theme.get(ColorType.UINimbusAlertYellow));
        theme.put(ColorType.TreeSpecialCommentChecker, new Color(187, 198, 4));
        theme.put(ColorType.TreeCompleteClassCommandChecker, theme.get(ColorType.UIText).brighter());
        theme.put(ColorType.TreeHotfixChecker, theme.get(ColorType.UINimbusInfoBlue));
        theme.put(ColorType.TreeOverwriterChecker, ((Color) theme.get(ColorType.UINimbusGreen)).brighter());
        theme.put(ColorType.TreePartialOverwriterChecker, ((Color) theme.get(ColorType.UINimbusGreen)).brighter());
        theme.put(ColorType.TreeOverwrittenChecker, theme.get(ColorType.UINimbusGreen));
        theme.put(ColorType.TreePartialOverwrittenChecker, ((Color) theme.get(ColorType.UINimbusGreen)).darker());
        theme.put(ColorType.TreeSyntaxError, theme.get(ColorType.UINimbusRed));
        theme.put(ColorType.TreeContentError, theme.get(ColorType.UINimbusOrange));
        theme.put(ColorType.TreeStyleWarning, Color.MAGENTA);
        theme.put(ColorType.TreeRootNode, Color.CYAN.darker());
        theme.put(ColorType.CodeText, theme.get(ColorType.UIText));
        theme.put(ColorType.CodeComment, new Color(0, 120, 0));
        theme.put(ColorType.CodeKeyword, theme.get(ColorType.UINimbusInfoBlue));
        theme.put(ColorType.CodeGDWord, theme.get(ColorType.UINimbusGreen));
        theme.put(ColorType.CodeMTWord, new Color(255, 0, 255));
        theme.put(ColorType.CodeNumber, theme.get(ColorType.UINimbusRed));
        theme.put(ColorType.CodeDoubleQuote, theme.get(ColorType.UINimbusOrange));
        theme.put(ColorType.CodeSingleQuote, theme.get(ColorType.UINimbusAlertYellow));
    }

    /**
     * Gets the theme with the given name.
     *
     * @param name the name of the theme to be returned
     * @return The theme with the given name
     */
    public static Theme getTheme(String name) {
        name = name.toLowerCase();
        Theme t = THEMES.get(name);
        if (t == null && name.endsWith(" mode")) {
            return THEMES.get(name.substring(0, name.length() - " mode".length()));
        }
        return t;
    }

    /**
     *
     * @return All currently available themes.
     */
    public static Collection<Theme> getAllInstalledThemes() {
        return THEMES.values();
    }

    /**
     * Returns the theme with the provided name, or creates such a theme if none
     * exists.
     *
     * @param name
     * @return
     */
    static Theme getThemeOrCreateIfNeeded(String name) {
        THEMES.putIfAbsent(name.toLowerCase(), new Theme(name));
        return getTheme(name);
    }

    /**
     * Returns the default theme
     *
     * @return The default theme
     */
    public static Theme getDefaultTheme() {
        return DEFAULT_THEME;
    }

    /**
     * Returns the current theme being used
     *
     * @return the current theme being used
     */
    public static Theme getTheme() {
        return curTheme;
    }

    /**
     * Returns the color used in the current theme, for a specific color type.
     *
     * @param colorType The color to look up
     * @return The Color
     */
    public static Color getColor(ColorType colorType) {
        return curTheme.get(colorType);
    }

    /**
     * Routine to initialize our TREE_ICON_MAP. We can't do this in the initial
     * static routine because it relies on MainGUI.INSTANCE existing, and we get
     * called far too early otherwise.
     */
    public static void initializeTreeIconMaps() {
        if (!initializedTreeIconMap
                && MainGUI.INSTANCE != null
                && MainGUI.INSTANCE.getMainTree() != null) {
            HashMap<String, ImageIcon> iconMap;
            for (Theme theme : getAllInstalledThemes()) {
                iconMap = theme.getIconMap();
                iconMap.put("Tree.collapsedIcon", nimbusTreeIcon(theme, false));
                iconMap.put("Tree.expandedIcon", nimbusTreeIcon(theme, true));
            }
            initializedTreeIconMap = true;
        }
    }

    /**
     * Returns an icon suitable for use in a Nimbus-themed JTree, using our own
     * coloration rather than the computed colors that Nimbus uses. In our Dark
     * theme, these icons are always black, which is nearly invisible against
     * the main tree background, and altering any of the Nimbus colors to make
     * them more visible also lightens other components which should remain
     * dark, hence this routine.
     *
     * One wrinkle is that SynthTreeUI.getExpandedIcon() is broken and doesn't
     * actually work. So if we want the expanded icon, we must use
     * getCollapsedIcon() instead (which does work), and manually rotate.
     *
     * Our Light theme doesn't actually need this, since the icons look fine on
     * there, but we'll use it anyway for consistency's sake.
     *
     * @param theme The theme to generate an icon for
     * @param expanded True if we are generating an "expanded" icon, False if we
     * are generating a "collapsed" icon.
     * @return The new icon
     */
    private static ImageIcon nimbusTreeIcon(Theme theme, boolean expanded) {

        CheckBoxTree mainTree = MainGUI.INSTANCE.getMainTree();
        SynthTreeUI tree = (SynthTreeUI) mainTree.getUI();
        Icon i = tree.getCollapsedIcon();

        // Set up our drawing environment
        int w = i.getIconWidth();
        int h = i.getIconHeight();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        BufferedImage image = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        Graphics2D g = image.createGraphics();
        AffineTransform oldTransform = g.getTransform();

        // Fill the new icon image with our text color
        g.setColor(theme.get(ThemeManager.ColorType.UITreeIcons));
        g.fill(new Rectangle2D.Double(0, 0, w, h));

        // Only pull the alpha channel from the icon
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));

        // Rotate, if we've been told to generate an "expanded" icon
        if (expanded) {
            g.rotate(Math.toRadians(90), 0, h);
            i.paintIcon(null, g, -h, 0);

            // Fill the rest of the image with full transparency
            g.setTransform(oldTransform);
            g.setColor(new Color(0, 0, 0, 0));
            g.fill(new Rectangle2D.Double(h, 0, w - h, h));
        } else {
            i.paintIcon(null, g, 0, 0);
        }

        g.dispose();

        return new ImageIcon(image);

    }

    /**
     * Sets the colors for the application for the given theme.
     *
     * @param theme Which theme to apply
     */
    public static void setTheme(Theme theme) {

        // Initialize icons
        initializeTreeIconMaps();

        // Set the theme map
        curTheme = theme;

        // Assign all the UI elements
        UIManager.put("control", theme.get(ColorType.UIControl));
        UIManager.put("info", theme.get(ColorType.UIInfo));
        UIManager.put("nimbusBase", theme.get(ColorType.UINimbusBase));
        UIManager.put("nimbusAlertYellow", theme.get(ColorType.UINimbusAlertYellow));
        UIManager.put("nimbusDisabledText", theme.get(ColorType.UINimbusDisabledText));
        UIManager.put("nimbusFocus", theme.get(ColorType.UINimbusFocus));
        UIManager.put("nimbusGreen", theme.get(ColorType.UINimbusGreen));
        UIManager.put("nimbusInfoBlue", theme.get(ColorType.UINimbusInfoBlue));
        UIManager.put("nimbusLightBackground", theme.get(ColorType.UINimbusLightBackground));
        UIManager.put("nimbusOrange", theme.get(ColorType.UINimbusOrange));
        UIManager.put("nimbusRed", theme.get(ColorType.UINimbusRed));
        UIManager.put("nimbusSelectedText", theme.get(ColorType.UINimbusSelectedText));
        UIManager.put("nimbusSelectionBackground", theme.get(ColorType.UINimbusSelectionBackground));
        UIManager.put("text", theme.get(ColorType.UIText));

        // Set our tree icons.  Note that we can't, apparently, do a
        // UIDefaults.put() on these properties, because there's some kind of
        // infernal delay as the properties get propagated to the tree, which
        // results in the changes often not actually being applied.  So instead
        // we've got to use Nimbus.Overrides and apply to the tree object
        // itself.
        if (initializedTreeIconMap) {
            CheckBoxTree tree = MainGUI.INSTANCE.getMainTree();
            UIDefaults map = new UIDefaults();
            for (Map.Entry<String, ImageIcon> entry : theme.getIconMap().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
            tree.putClientProperty("Nimbus.Overrides", map);
        }

    }

}
