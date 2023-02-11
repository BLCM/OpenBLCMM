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
package blcmm.gui;

import blcmm.utilities.Options;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An enum that keeps track on how often someone has seen a certain popup.
 * Should mostly be used for popups after an update or first time after opening
 * specific a specific window.
 *
 *
 * @author LightChaosman
 */
public enum PopupType {
    //The sum of the number of required bits to store <code>maxSeen</code> among all enum values may not exceed 32 bits at the moment.
    //Once it does, we need either a LongOption, *or* just make it a stringOption, and handle the bit management differently
    //BigInteger would allow us to keep using this scheme.
    //Just using a stringlist with the enum constant names would scale to large
    TOOLTIP_CONFIRMER;

    //To test this, we put in this bit of logic
    static {
        assert Arrays.stream(PopupType.values()).collect(Collectors.summingInt((popup) -> popup.getBitCount())) <= 32;
        //Once this fails, we will need a different datatype to store our flags.
    }

    //This is here just for testing
    public static void resetAll() {
        Arrays.stream(PopupType.values()).forEach(PopupType::resetSeenCount);
    }
    //Could initialize this in the constructors.
    //But, if there's a JVM out there whose enum initialization order is non-deterministic,
    //this would give bad results
    private transient int leastSignificantBitIndex = -1;
    private final int maxSeen;
    private final boolean enabled;

    private PopupType() {
        this(1);
    }

    private PopupType(int maxSeen) {
        this(maxSeen, true);
    }

    private PopupType(int maxSeen, boolean enabled) {
        this.maxSeen = maxSeen;
        this.enabled = enabled;
    }

    public boolean isAvailable() {
        return enabled && getSeenCount() < getMaxSeenCount();
    }

    public void increaseSeenCount() {
        this.setSeenCount(getSeenCount() + 1);
    }

    public void resetSeenCount() {
        this.setSeenCount(0);
    }

    private void setSeenCount(int i) {
        if (i < 0 || i > getMaxSeenCount()) {
            throw new IllegalArgumentException(i + " is not between 0 and " + getMaxSeenCount());
        }
        int status = Options.INSTANCE.getPopupStatus();
        status &= ~getMask(); //clear our bits
        status |= i << getFirstBit(); //set our new bits
        Options.INSTANCE.setPopupStatus(status);
    }

    private int getMaxSeenCount() {
        return maxSeen;
    }

    private int getSeenCount() {
        return (Options.INSTANCE.getPopupStatus() & this.getMask()) >> getFirstBit();
    }

    private int getMask() {
        return ((1 << getBitCount()) - 1) << this.getFirstBit();
    }

    private int getFirstBit() {
        if (leastSignificantBitIndex == -1) {
            if (ordinal() == 0) {
                leastSignificantBitIndex = 0;
            } else {
                PopupType prev = PopupType.values()[ordinal() - 1];
                leastSignificantBitIndex = prev.getFirstBit() + prev.getBitCount();
            }
        }
        return leastSignificantBitIndex;
    }

    private int getBitCount() {
        return (int) (Math.log(getMaxSeenCount()) / Math.log(2) + 1);
    }

}
