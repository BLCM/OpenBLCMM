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

package blcmm.utilities;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import javax.imageio.ImageIO;

/**
 * Class to manage icons that the game uses.  This class was
 * reimplemented based on the calls BLCMM made into BLCMM_Utilities.jar,
 * without reference to the original sourcecode.
 *
 * @author apocalyptech
 */
public class IconManager {

    static final String BLCMM_ICON_PATH = "/resources/Icon.png";
    static final int BLANK_DEFAULT_RES = 256;
    private final static HashMap<String, HashMap<Integer, BufferedImage>> iconCache = new HashMap<> ();

    /**
     * Returns the specified icon, or an "empty" image if the path can't be
     * found or loaded.
     *
     * @param path The resource path to the icon
     * @return The icon for the game
     */
    public static BufferedImage getIcon(String path) {
        return IconManager.getIcon(path, -1);
    }

    /**
     * Returns the specified icon, scaled to the given width/height.  If the
     * size is given as -1, the icon will be returned in its "native"
     * resolution.
     *
     * @param path The resource path to the icon
     * @param size The size to scale to -- images are assumed to be square
     * @return The icon for the game
     */
    public static BufferedImage getIcon(String path, int size) {

        // If we haven't seen this path at all, load in its default resolution
        if (!IconManager.iconCache.containsKey(path)) {
            BufferedImage im = null;
            try {
                InputStream stream = IconManager.class.getResourceAsStream(path);
                if (stream != null) {
                    im = ImageIO.read(stream);
                }
            } catch (IOException e) {
                // Whatever, just default to our blank fallback
            }
            if (im == null) {
                im = new BufferedImage(IconManager.BLANK_DEFAULT_RES,
                        IconManager.BLANK_DEFAULT_RES,
                        BufferedImage.TYPE_INT_ARGB);
            }
            IconManager.iconCache.put(path, new HashMap<> ());
            IconManager.iconCache.get(path).put(-1, im);
        }

        // If we haven't seen this *size* at all, do a resize.  We know that
        // -1 is already in here since we populated it above, so we'll never
        // try to resize to -1x-1.  Also, if the requested size is already
        // the size of the image, skip trying to resize and just return the
        // original.
        if (!IconManager.iconCache.get(path).containsKey(size)) {
            BufferedImage fullImage = IconManager.iconCache.get(path).get(-1);
            if (fullImage.getWidth() == size && fullImage.getHeight() == size) {
                IconManager.iconCache.get(path).put(size, fullImage);
            } else {
                // getScaledInstance produces good-looking rescales but returns
                // an `Image` instead of a `BufferedImage`.  `Image` would be
                // fine except that it doesn't actually support sensibly getting
                // the image dimensions, which I want to check above?  WTF.
                // I tried scaling the image directly to a BufferedImage using
                // some Graphics2D techniques but it always looked like dogshit,
                // so instead I'm just going back to getScaledInstance and then
                // copying the damn thing over.  It sounds from The Internet
                // that getScaledInstance isn't well thought-of, but eh.
                Image resized = fullImage.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                BufferedImage buffered = new BufferedImage(size, size, fullImage.getType());
                Graphics2D g = buffered.createGraphics();
                g.drawImage(resized, 0, 0, null);
                g.dispose();
                IconManager.iconCache.get(path).put(size, buffered);
            }
        }

        // If we got here, we should have *something*, even if it's just blank.
        return IconManager.iconCache.get(path).get(size);
    }

    /**
     * Returns the full-size OpenBLCMM Icon itself
     *
     * @return The OpenBLCMM icon
     */
    public static BufferedImage getBLCMMIcon() {
        return getIcon(BLCMM_ICON_PATH);
    }

    /**
     * Returns the OpenBLCMM icon, scaled to the specified size
     *
     * @param size The width and height of the requested icon (assumed to be square)
     * @return The scaled OpenBLCMM icon
     */
    public static BufferedImage getBLCMMIcon(int size) {
        return getIcon(BLCMM_ICON_PATH, size);
    }

}
