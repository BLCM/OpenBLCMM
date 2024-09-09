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
import blcmm.model.properties.PropertyChecker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author LightChaosman
 */
public class TransientModelData {

    private final ModelElement element;
    private final HashSet<PropertyChecker> myProperties = new HashSet<>();
    private final Map<PropertyChecker, Integer> properties = new TreeMap<>(GlobalListOfProperties.PROPERTY_COMPARATOR);

    private boolean lostParent;

    /**
     * Enum to hold some state as to whether this element is
     * overwriting/overwritten, etc. This is only actually used to provide
     * tooltips to the user, at time of writing.
     */
    public enum OverwriteState {
        Normal,
        Overwritten,
        PartialOverwritten,
        Overwriter,
        PartialOverwriter,
    }

    private OverwriteState overwriteState;

    /**
     * Boolean to control whether or not this element accepts statuses from our
     * various checkers. Used to prevent the top-level and "mods" folders from
     * taking on statuses of their own.
     */
    private boolean acceptStatuses;

    TransientModelData(ModelElement element) {
        PropertyChecker.Hints hints = null;
        if (element instanceof SetCommand) {
            hints = new PropertyChecker.Hints(((SetCommand) element).getObject().toLowerCase(),
                    ((SetCommand) element).getField().toLowerCase(),
                    ((SetCommand) element).getValue().toLowerCase());
        }
        for (PropertyChecker checker : GlobalListOfProperties.LIST) {
            boolean check = hints == null ? checker.checkProperty(element) : checker.checkProperty(element, hints);
            if (check) {
                properties.put(checker, 1);
                myProperties.add(checker);
            }
        }
        this.overwriteState = OverwriteState.Normal;
        this.element = element;
        this.acceptStatuses = true;
        this.lostParent = false;
    }

    public Set<PropertyChecker> getProperties() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    /**
     * Returns the number of times the element of this transient data has the
     * given property, trough its descendants.
     *
     * @param property the class of the property
     * @return
     */
    public int getNumberOfOccurences(Class<? extends PropertyChecker> property) throws IllegalStateException {
        List<PropertyChecker> l = GlobalListOfProperties.CLASS_TO_INSTANCE_MAP.get(property);
        if (l == null || l.size() != 1) {
            throw new IllegalStateException("This method is only to be used if there is but a single instance of the given checker class");
        }
        return getNumberOfOccurences(l.get(0));
    }

    /**
     * Returns the number of times the element of this transient data has the
     * given property, trough its descendants.
     *
     * @param property
     * @return
     */
    public int getNumberOfOccurences(PropertyChecker property) {
        Integer x = properties.get(property);
        return x == null ? 0 : x;
    }

    /**
     * Returns a summary string detailing the various checker properties which
     * are stored inside this transient data, intended for appending to items in
     * the tree for some easier visual debugging. Probably not to ever be used
     * outside of debugging.
     *
     * @return A string for reporting purposes
     */
    public String summaryString() {
        ArrayList<String> al = new ArrayList<>();
        for (Map.Entry<PropertyChecker, Integer> entry : properties.entrySet()) {
            al.add(String.format("%s: %d", entry.getKey(), entry.getValue()));
        }
        return String.format("[%s]", String.join(", ", al));
    }

    /**
     * Disables this TransientModelData object from actually taking any status
     * from our various sanity checks. This way the GUI won't highlight the
     * element with color or tooltips. Setting this on an element in the middle
     * of a tree is not recommended and will result in undefined behavior --
     * this is intended to be used from the root of the tree (such as the main
     * root folder, and the "mods" folder).
     */
    public void disableStatuses() {
        this.acceptStatuses = false;
        properties.clear();
        myProperties.clear();
    }

    /**
     * Get our OverwriteState, used primarily to let our renderer display a
     * tooltip.
     *
     * @return the overwriteState
     */
    public OverwriteState getOverwriteState() {
        return overwriteState;
    }

    /**
     * Sets our OverwriteState, used primarily to let our renderer display a
     * tooltip.
     *
     * @param overwriteState the overwriteState to set
     * @return True if the overwrite state is settable, false otherwise.
     */
    public boolean setOverwriteState(OverwriteState overwriteState) {
        if (this.acceptStatuses) {
            this.overwriteState = overwriteState;
            return true;
        } else {
            return false;
        }
    }

    void updateByInsertingNewChild(ModelElement newChild) {
        updateByChangingChildArray(newChild, true);
    }

    void updateByRemovingChild(ModelElement oldChild) {
        updateByChangingChildArray(oldChild, false);
    }

    void updateByChangingOwnProperty(PropertyChecker... checkers) {
        for (PropertyChecker property : checkers) {
            updateSelfProperty(property);
        }
    }

    private void updateByChangingChildArray(ModelElement newChild, boolean add) {
        assert element instanceof ModelElementContainer;

        // First check all our PropertyCheckers against ourselves and set all
        // relevant info
        for (PropertyChecker property : GlobalListOfProperties.DEPENDING_ON_CHILDREN) {
            updateSelfProperty(property);
        }

        // Now take any properties from our new child and propagate them to
        // ourselves, if needed.
        TransientModelData childData = newChild.transientData;
        for (PropertyChecker property : childData.properties.keySet()) {
            if (property.isPropagatingToAncestors()) {
                propagate(property, childData, add ? 1 : -1);
            }
        }
    }

    private void propagate(PropertyChecker property, TransientModelData childData, int fac) {
        int value = fac * childData.properties.get(property);
        propagate(property, value);

    }

    private void propagate(PropertyChecker property, int value) {
        ModelElement container = element;
        while (container != null) {
            TransientModelData containerdata = container.transientData;
            if (containerdata.acceptStatuses) {
                containerdata.properties.put(property, containerdata.properties.getOrDefault(property, 0) + value);
                if (containerdata.properties.get(property) == 0) {
                    containerdata.properties.remove(property);
                }
            }
            container = container.getParent();
        }
    }

    private void updateSelfProperty(PropertyChecker property) {
        boolean newp = property.checkProperty(element);
        boolean oldp = myProperties.contains(property);
        if (newp != oldp) {
            if (this.acceptStatuses) {
                if (newp) {
                    myProperties.add(property);
                } else {
                    myProperties.remove(property);
                }
            }
            if (property.isPropagatingToAncestors()) {
                propagate(property, newp ? 1 : -1);
            } else {
                if (this.acceptStatuses) {
                    if (newp) {
                        properties.put(property, 1);
                    } else {
                        properties.remove(property);
                    }
                }
            }
        }
    }

}
