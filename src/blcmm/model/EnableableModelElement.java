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
package blcmm.model;

import blcmm.model.properties.GlobalListOfProperties;
import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author LightChaosman
 */
abstract class EnableableModelElement extends ModelElement {

    private final HashSet<Profile> onInProfiles = new HashSet<>();
    private boolean selected = false;

    /**
     * Returns whether this code is selected.
     *
     * @return
     */
    public boolean isSelected() {
        return selected;
    }

    final void turnOnInProfile(Profile p) {
        if (p == null) {
            throw new NullPointerException();
        }
        this.onInProfiles.add(p);
    }

    final void turnOffInProfile(Profile p) {
        if (p == null) {
            throw new NullPointerException();
        }
        this.onInProfiles.remove(p);
    }

    protected final void copyProfilesAndSelectedFrom(EnableableModelElement el) {
        onInProfiles.clear();
        onInProfiles.addAll(el.onInProfiles);
        selected = el.selected;
        transientData.updateByChangingOwnProperty(GlobalListOfProperties.CLASS_TO_INSTANCE_MAP.get(GlobalListOfProperties.LeafSelectedChecker.class).get(0));
    }

    final void profileChanged(Profile p) {
        if (p == null) {
            throw new NullPointerException();
        }
        this.selected = onInProfiles.contains(p);
        transientData.updateByChangingOwnProperty(GlobalListOfProperties.CLASS_TO_INSTANCE_MAP.get(GlobalListOfProperties.LeafSelectedChecker.class).get(0));
    }

    protected final String getProfileString() {
        StringBuilder sb = new StringBuilder();
        sb.append("profiles=\"");
        boolean first = true;
        for (Profile prof : onInProfiles) {
            if (!first) {
                sb.append(",");
            }
            sb.append(prof.getName());
            first = false;
        }
        sb.append("\"");
        return sb.toString();
    }

    final Collection<Profile> getProfiles() {
        return onInProfiles;
    }

}
