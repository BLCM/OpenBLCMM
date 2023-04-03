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
package blcmm;

/**
 * Just some general application metadata -- ie: app name + version
 *
 * @author pez
 */
public class Meta {

    /**
     * The application name, used in text throughout the app.
     */
    public static final String NAME = "OpenBLCMM";

    /**
     * App version.
     */
    public static final String VERSION = "1.3.0";

    /**
     * User data directory for storing prefs, extracted data, etc.  We could
     * almost certainly just use NAME for this, but if the name ever gets
     * changed to something with a space or special chars or something, it might
     * be nice to have a "normalized" version for the dir name.
     */
    public static final String APP_DATA_DIR_NAME = "OpenBLCMM";

    /**
     * The name of the compiled Jar file.  This may end up getting factored
     * out, since I don't think there's much actual use for it anymore, and
     * it wouldn't be accurate for the Windows-compiled version anyway.
     */
    public static final String JARFILE = "OpenBLCMM.jar";

}
