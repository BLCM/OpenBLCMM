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
    public static final String VERSION = "1.3.0-beta.2";

    /**
     * User data directory for storing prefs, extracted data, etc.  We could
     * almost certainly just use NAME for this, but if the name ever gets
     * changed to something with a space or special chars or something, it might
     * be nice to have a "normalized" version for the dir name.
     */
    public static final String APP_DATA_DIR_NAME = "OpenBLCMM";

    /**
     * URL to the project source control.
     *
     * We're defining a bunch of URLs here which could, at the moment, be
     * derived from each other, but I didn't want these to be intrinsically
     * GitHub-specific.
     */
    public static final String CODE_URL = "https://github.com/BLCM/OpenBLCMM";

    /**
     * URL to the project releases.
     *
     * We're defining a bunch of URLs here which could, at the moment, be
     * derived from each other, but I didn't want these to be intrinsically
     * GitHub-specific.
     */
    // TEMP: Testing out how users find the release-download process
    //public static final String RELEASES_URL = "https://github.com/BLCM/OpenBLCMM/releases";
    public static final String RELEASES_URL = "https://github.com/apocalyptech/OpenBLCMM-TestBed/releases";

    /**
     * URL to where to submit bugs, shown on the crash handler dialog.
     *
     * We're defining a bunch of URLs here which could, at the moment, be
     * derived from each other, but I didn't want these to be intrinsically
     * GitHub-specific.
     */
    public static final String BUGREPORT_URL = "https://github.com/BLCM/OpenBLCMM/issues";

    /**
     * URL to where the app can retrieve the latest version of OpenBLCMM
     * available.  This should just be a plain text file with only a version
     * number in it, nothing more.  Note that the URL here is entirely just
     * for testing purposes; I intend to host it off of github eventually.
     */
    // More testing.  Store the version file right on the github repo, maybe?
    //public static final String UPDATE_VERSION_URL = "https://apocalyptech.com/scratchpad/openblcmm-latest.txt";
    public static final String UPDATE_VERSION_URL = "https://raw.githubusercontent.com/apocalyptech/OpenBLCMM-TestBed/main/openblcmm-latest.txt";

}
