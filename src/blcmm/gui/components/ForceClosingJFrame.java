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

import blcmm.utilities.GlobalLogger;
import java.util.HashSet;
import javax.swing.JFrame;

/**
 *
 * @author LightChaosman
 */
public class ForceClosingJFrame extends JFrame {

    private final static HashSet<ForceClosingJFrame> INSTANCES = new HashSet<>();

    public ForceClosingJFrame() {
        super();
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        INSTANCES.add(this);
    }

    public ForceClosingJFrame(String name) {
        super(name);
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GlobalLogger.log("Opening frame: " + name);
        INSTANCES.add(this);
    }

    @Override
    public void dispose() {
        GlobalLogger.log("Closing frame: " + getTitle());
        super.dispose();
        INSTANCES.remove(this);
        if (INSTANCES.isEmpty()) {
            GlobalLogger.deleteLog();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
            System.exit(0);
        }
    }

}
