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

package blcmm.model;

import blcmm.utilities.GlobalLogger;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to help with various hotfix conversion needs.  This class was
 * reimplemented based on the calls BLCMM makes into BLCMM_Utilities.jar,
 * without reference to the original sourcecode.
 *
 * This class used to live under blcmm.utilities, but my new reimplementations
 * require access to ModelElement.setParent, which as-is is only accessible
 * from other members of blcmm.model.  So, it got moved over here.  Possibly
 * that makes more sense anyway?
 *
 * @author apocalyptech
 */
public class HotfixConverter {

    private final static Pattern HOTFIX_KEY_PATTERN = Pattern.compile("^(?<prefix>.+?)(-(?<name>.+?))?(?<count>\\d+)$");

    /**
     * A single hotfix key/value pair.  Really this could just be a Tuple or
     * something, but whatever.
     */
    public class HotfixKeyValuePair {

        public String key;
        public String value;

        public HotfixKeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }

    }

    /**
     * Class to manage hotfix number suffixes.  Each hotfix needs to
     * have a unique name, so this class remembers the names an increments
     * a number by 1 for each time it's seen.  Technically we should
     * maybe include the hotfix type in the hash as well, since a Level
     * hotfix and an OnDemand hotfix with the same name could use separate
     * numbering.  I don't actually care enough to bother with that, though.
     */
    public class HotfixCountManager {

        private final HashMap<String, Integer> hotfixCounts;

        public HotfixCountManager() {
            this.hotfixCounts = new HashMap<>();
        }

        public int next(String name) {
            if (name == null || name.equals("")) {
                name = "Hotfix";
            }
            String name_lower = name.toLowerCase();
            int cur_number = this.hotfixCounts.getOrDefault(name_lower, 0);
            cur_number++;
            this.hotfixCounts.put(name_lower, cur_number);
            return cur_number;
        }

    }

    private final HotfixCountManager hotfixCounts;

    public HotfixConverter() {
        this.hotfixCounts = new HotfixCountManager();
    }

    /**
     * Given a HotfixCommand, generate the Key/Value pair suitable for inclusion in an
     * exported mod file.
     *
     * @param command The actual HotfixCommand to be written out.
     * @return The key/value pair
     */
    public HotfixKeyValuePair getKeyValuePair(HotfixCommand command) {
        HotfixWrapper wrapper = command.getParent();
        String hotfixPrefix = wrapper.getType().getPrefix();
        // TODO: I actually want to *not* use hotfix names anymore in the actual key.  There's
        // a lot of sus names out there which I'm honesly kind of surprised work.  At the very
        // least, should strip out non-alphanumerics.
        int hotfixNumber = this.hotfixCounts.next(wrapper.getName());
        String key = hotfixPrefix + "-" + wrapper.getName() + Integer.toString(hotfixNumber);
        String value = command.toRawHotfixFormat();
        return new HotfixKeyValuePair(key, value);
    }

    /**
     * Given a hotfix key and value (for instance, from a FilterTool-formatted mod file),
     * convert them to a new HotfixWrapper object.
     *
     * @param key The hotfix key
     * @param value The hotfix value
     * @return A new HotfixWrapper object
     */
    public static HotfixWrapper keyAndValuetoNewWrapper(String key, String value) {

        // Parse the key
        // TODO: eh, regexes are slow...
        Matcher m = HotfixConverter.HOTFIX_KEY_PATTERN.matcher(key);
        if (!m.matches()) {
            GlobalLogger.log("Invalid hotfix key: " + key);
            return null;
        }
        String detectedPrefix = m.group("prefix");
        String detectedName = m.group("name");
        if (detectedName == null) {
            detectedName = detectedPrefix;
        }
        HotfixType detectedType = HotfixType.getByPrefix(detectedPrefix);
        if (detectedType == null) {
            GlobalLogger.log("Unknown hotfix key prefix: " + detectedPrefix);
            return null;
        }

        // Figure out what we expect out of the hotfix
        int maxSplits;
        boolean hasParam;
        if (detectedPrefix.equalsIgnoreCase("SparkPatchEntry")) {
            maxSplits = 4;
            hasParam = false;
        } else {
            maxSplits = 5;
            hasParam = true;
        }

        // Split apart the hotfix
        String[] valueParts = value.split(",", maxSplits);
        if (valueParts.length != maxSplits) {
            GlobalLogger.log("Unknown hotfix value: " + value);
            return null;
        }

        // ... and parse its various bits.
        String param = null;
        int curIndex = 0;
        if (hasParam) {
            param = valueParts[curIndex++];
            if (param.equals("")) {
                param = "None";
            }
        }
        String object = valueParts[curIndex++];
        String attr = valueParts[curIndex++];
        String prevValue = valueParts[curIndex++];
        String newValue = valueParts[curIndex++];

        // Construct a new command with the info
        HotfixCommand newCommand;
        if (prevValue.equals("")) {
            newCommand = new HotfixCommand(object, attr, newValue);
        } else {
            newCommand = new SetCMPCommand(object, attr, prevValue, newValue);
        }

        // Validate.  This feels a *bit* silly since we're calling getCode() to generate, but if
        // there are unexpectedly empty fields in valueParts, this should catch it.
        try {
            SetCommand.validateCommand(newCommand.getCode(), true);
        } catch (Exception e) {
            GlobalLogger.log("Invalid hotfix: " + newCommand.getCode());
            return null;
        }

        // Now wrap it all up and return!
        HotfixWrapper wrapper = new HotfixWrapper(detectedName, detectedType, param);
        newCommand.setParent(wrapper);
        wrapper.addElement(newCommand);
        return wrapper;
    }

}
