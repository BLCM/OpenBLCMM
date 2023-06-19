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
package blcmm.gui.tree;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 *
 * @author FromDarkHell
 */
public class EasterEggs {

    public static class CheatCode {

        public static CheatCode KONAMI = new CheatCode(
                KeyEvent.VK_UP, KeyEvent.VK_UP,
                KeyEvent.VK_DOWN, KeyEvent.VK_DOWN,
                KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                KeyEvent.VK_B, KeyEvent.VK_A);

        private final int[] sequenceOfKeyEvents;
        private transient int progress = 0;

        public CheatCode(int... sequenceOfKeyEvents) {
            this.sequenceOfKeyEvents = sequenceOfKeyEvents;
        }

        public boolean keyPressCompletedCode(int keyCode) {
            if (keyCode != sequenceOfKeyEvents[progress]) {
                // Reset our index since the code was wrong.
                progress = 0;
                return false;
            }

            // Increase our index by one
            progress++;

            // This happens when our final button is pressed, VK_B in this case.
            if (progress == sequenceOfKeyEvents.length) {
                // Reset our index since it was right to fix an array index out of bounds exception.
                progress = 0;
                return true;
            }
            return false;
        }

    }

    public void checkAllCodes(int keyPressed) {
        // You put all other input comparisons in this function.
        boolean keyPressCompletedCode = CheatCode.KONAMI.keyPressCompletedCode(keyPressed);
        if (keyPressCompletedCode) {
            try {
                // SkiFree bitches.  (Not using Utilities.launchBrowser() here
                // because it'd be ridiculous to throw a modal dialog in the
                // event of error, for something like this.
                URL skiFree = new URI("https://basicallydan.github.io/skifree.js/").toURL();
                Desktop.getDesktop().browse(skiFree.toURI());
            } catch (URISyntaxException | IOException | UnsupportedOperationException ex) {
            }
        }
    }

}
