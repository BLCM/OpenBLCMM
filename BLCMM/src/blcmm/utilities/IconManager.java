/*
 * Copyright (C) 2023 CJ Kucera
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

package blcmm.utilities;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Class to manage icons that the game uses.  This class was
 * reimplemented based on the calls BLCMM makes into BLCMM_Utilities.jar,
 * without reference to the original sourcecode.
 * 
 * @author apocalyptech
 */
public class IconManager {
    
    static final String BLCMM_ICON_PATH = "/resources/Icon.png";
    
    /**
     * Returns the specified icon, or an "empty" image if the path can't be
     * found or loaded.
     * 
     * @param path The resource path to the icon
     * @return The icon for the game
     */
    public static Image getIcon(String path) {
        try {
            InputStream stream = IconManager.class.getResourceAsStream(path);
            if (stream != null) {
                Image im = ImageIO.read(stream);
                if (im != null) {
                    return im;
                }
            }
        } catch (IOException e) {
            // Whatever, just default to our blank fallback
        }
        return new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
    }
    
    /**
     * Returns the specified icon, scaled to the given width/height.
     * 
     * @param path The resource path to the icon
     * @param size The size to scale to -- images are assumed to be square
     * @return The icon for the game
     */    
    public static Image getIcon(String path, int size) {
        Image im = getIcon(path);
        return im.getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }

    /**
     * Returns the full-size BLCMM Icon itself
     * 
     * @return The BLCMM icon
     */
    public static Image getBLCMMIcon() {
        return getIcon(BLCMM_ICON_PATH);
    }

    /**
     * Returns the BLCMM icon, scaled to the specified size
     * 
     * @param size The width and height of the requested icon (assumed to be square)
     * @return The scaled BLCMM icon
     */
    public static Image getBLCMMIcon(int size) {
        return getIcon(BLCMM_ICON_PATH, size);
    }

}
