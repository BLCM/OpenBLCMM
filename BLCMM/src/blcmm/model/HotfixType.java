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

package blcmm.model;

import java.util.HashMap;

/**
 * Enum to describe the hotfix types available for OpenBLCMM.  This class was
 * reimplemented based on the calls BLCMM made into BLCMM_Utilities.jar,
 * without reference to the original sourcecode.
 *
 * @author apocalyptech
 */
public enum HotfixType {

    // Enum members
    PATCH("SparkPatchEntry"),
    LEVEL("SparkLevelPatchEntry"),
    ONDEMAND("SparkOnDemandPatchEntry");

    /**
     * Convenience var to provide a mapping of hotfix prefixes to type
     */
    private static final HashMap<String, HotfixType> PREFIX_MAP;

    static {
        PREFIX_MAP = new HashMap<>();
        for (HotfixType type : HotfixType.values()) {
            PREFIX_MAP.put(type.getPrefix().toLowerCase(), type);
        }
    }

    private final String hotfixPrefix;

    private HotfixType(String hotfixPrefix) {
        this.hotfixPrefix = hotfixPrefix;
    }

    public String getPrefix() {
        return this.hotfixPrefix;
    }

    /**
     * Given a hotfix key prefix, return the correct HotfixType if possible.
     *
     * @param prefix The key prefix
     * @return The HotfixType matching that prefix
     */
    public static HotfixType getByPrefix(String prefix) {
        String prefix_lower = prefix.toLowerCase();
        if (PREFIX_MAP.containsKey(prefix_lower)) {
            return PREFIX_MAP.get(prefix_lower);
        } else {
            return null;
        }
    }

}
