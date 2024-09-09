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
     * App version.  Should follow https://semver.org/ conventions.
     */
    public static final String VERSION = "1.4.1";

    /**
     * User data directory for storing prefs, extracted data, etc.  If NAME
     * ever acquires special characters or spaces or whatever, it might be nice
     * to use a "sanitized" version here.
     */
    public static final String APP_DATA_DIR_NAME = NAME;

    /**
     * URL to the project source control.
     */
    public static final String CODE_URL = "https://github.com/BLCM/OpenBLCMM";

    /**
     * URL to the project releases.
     */
    public static final String RELEASES_URL = CODE_URL + "/releases";

    /**
     * URL to where to submit bugs, shown on the crash handler dialog.
     */
    public static final String BUGREPORT_URL = CODE_URL + "/issues";

    /**
     * URL to where the app can retrieve the latest version of OpenBLCMM (and
     * its datapacks) available.  Has a colon-suffixed "key" to indicate the
     * component whose version is being reported (valid vlaues: OpenBLCMM,
     * BL2Data, TPSData, or AODKData), and the data to the right of the colon
     * is the version string.  The *Data versions have two values separated
     * by a comma; the first is the database (schema) version, the second being
     * the data version.
     */
    public static final String UPDATE_VERSION_URL = "https://raw.githubusercontent.com/BLCM/OpenBLCMM/main/openblcmm-latest.txt";

    /**
     * URL where data pack downloads are available.
     */
    public static final String DATA_DOWNLOAD_URL = "https://github.com/BLCM/OpenBLCMM-Data/releases";

}
