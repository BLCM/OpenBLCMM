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

/**
 * A class to handle the date-versioned version numbers that our OE Datapacks
 * use.  They're in the form YYYY.MM.DD.NN, with the date as the first
 * components, and then a raw number just in case we end up releasing more
 * than once per day.
 *
 * This class blindly assumes that the year is four digits and the rest of
 * the "fields" are two digits, for purposes of comparing versions.  That's
 * obviously quite naive, but I'm honestly not concerned about edge cases, since
 * the only places we get these version strings from are from "official"
 * sources, and the only failure condition is related to new-version
 * notifications.
 *
 * @author apocalyptech
 */
public class DateVersion {

    private final String rawVersion;
    private int year;
    private int month;
    private int day;
    private int num;
    private int compareNumber;

    public DateVersion(String rawVersion) {
        this.rawVersion = rawVersion;
        String[] parts = rawVersion.split("\\.", 4);
        if (parts.length != 4) {
            throw new RuntimeException("Invalid number of integer parts for raw string: " + rawVersion);
        }
        this.year = Integer.parseInt(parts[0]);
        this.month = Integer.parseInt(parts[1]);
        this.day = Integer.parseInt(parts[2]);
        this.num = Integer.parseInt(parts[3]);
        this.compareNumber = this.year*1000000 + this.month*10000 + this.day*100 + this.num;
    }

    @Override
    public String toString() {
        return this.rawVersion;
    }

    public boolean isGreaterThan(DateVersion other) {
        return this.compareNumber > other.compareNumber;
    }

}
