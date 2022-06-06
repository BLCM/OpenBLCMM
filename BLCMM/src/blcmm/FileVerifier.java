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
 */
package blcmm;

import blcmm.utilities.Utilities;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.swing.JOptionPane;

/**
 * Janky self-adapting class
 *
 * @author LightChaosman
 */
class FileVerifier {

    private static final String[][] FILES_TO_VERIFY = {
        {"BLCMM_Data_Interaction_Library", "e9f94ef10b1ef66f17e626648289a38d760d5dd745cc8d8f80e454cb5c812c45"},
        {"BLCMM_Resources",/*           */ "f40fc4f1c46236f7d914618ca46c8e78858b39601fb444e7f92d7406430b845a"},
        {"BLCMM_Utilities",/*           */ "c9958fc150ff45e5bd9be314690b395693859c2b6f4282ae2078789b186a9303"},};

    static boolean verifyFiles() {
        boolean missing = false, corrupt = false, changed = false, ex2 = false;
        for (String[] file : FILES_TO_VERIFY) {
            try {
                File f = new File("lib/" + file[0] + ".jar");
                if (!f.exists()) {
                    missing = true;
                } else {
                    String sha256 = Utilities.sha256(f);
                    if (!sha256.equals(file[1])) {
                        if (Utilities.isCreatorMode()) {
                            changed = true;
                            Utilities.writeStringToFile(Utilities.readFileToString(ME()).replace(file[1], sha256), ME());
                        } else {
                            corrupt = true;
                            f.delete();
                            f.deleteOnExit();//If they are corrupt, assume they're not loaded into the JVM, so they can actually be deleted
                        }
                    }
                }
            } catch (IOException | NoSuchAlgorithmException ex) {
                ex2 = true;
                ex.printStackTrace();
            }
        }
        if (changed && !missing && !corrupt && !ex2) {
            JOptionPane.showMessageDialog(null, "FileVerifier.java has been updated");
        } else if (!(!missing && !corrupt && !ex2)) {
            JOptionPane.showMessageDialog(null,
                    "It seems your BLCMM download was incomplete.\n"
                    + "Please restart the launcher to attempt to complete it.",
                    "Incomplete download detected", JOptionPane.ERROR_MESSAGE);
        }
        return !missing && !corrupt && !ex2;
    }

    private static File ME() {
        return new File("src/" + FileVerifier.class.getName().replace(".", "/") + ".java");
    }

}
