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
package blcmm.model;

import java.util.Comparator;
import java.util.LinkedList;

/**
 * The superclass of the three main components of our model
 *
 * @author LightChaosman
 */
public abstract class ModelElement {

    public final static Comparator<ModelElement> ELEMENT_COMPERATOR = (ModelElement o1, ModelElement o2) -> {
        if (o1 instanceof Comment) {
            if (o2 instanceof Comment) {
                return 0;
            } else {
                return 1;
            }
        }
        if (o2 instanceof Comment) {
            return 1;
        } else if (o1 instanceof Category) {
            if (o2 instanceof Category) {
                return ((Category) o1).getName().compareTo(((Category) o2).getName());
            } else {
                return 1;
            }
        } else if (o2 instanceof Category) {
            return -1;
        } else if (o1 instanceof HotfixWrapper) {
            if (o2 instanceof HotfixWrapper) {
                return ((HotfixWrapper) o1).getName().compareTo(((HotfixWrapper) o2).getName());
            } else {
                return 1;
            }
        } else if (o2 instanceof HotfixWrapper) {
            return -1;
        }
        return o1.toString().compareTo(o2.toString());
    };

    private static LinkedList<ModelElementContainer> getMUTAncestorPath(ModelElement el, LinkedList<ModelElementContainer> path) {
        path.add(el.parent);
        if (el.parent == null) {
            return null;
        }
        if (el.parent instanceof Category && ((Category) el.parent).isMutuallyExclusive()) {
            return path;
        }
        return getMUTAncestorPath(el.parent, path);
    }

    private ModelElementContainer parent;
    protected transient TransientModelData transientData;

    public TransientModelData getTransientData() {
        return transientData;
    }

    protected abstract String toXMLString();

    void setParent(ModelElementContainer newParent) {
        if (this.parent != null && this.parent.getElements().contains(this)) {
            throw new IllegalStateException("remove this element from its current parent before allocating it to a new one");
        }
        if (newParent == this) {
            throw new IllegalArgumentException("A container can't be its own parent");
        }
        this.parent = newParent;
        if (newParent != null && this instanceof SetCommand) {
            newParent.updateLengths((SetCommand) this);
        }
    }

    /**
     * Returns the category containing this Code, or null if this is the root
     * category.
     *
     * @return
     */
    public ModelElementContainer getParent() {
        return parent;
    }

    public abstract ModelElement copy();

    public boolean hasLockedAncestor() {
        if (this instanceof Category && ((Category) this).isLocked()) {
            return true;
        }
        if (getParent() != null) {
            return getParent().hasLockedAncestor();
        }
        return false;
    }

    public boolean hasMUTAncestor() {
        return getMUTAncestorPath() != null;
    }

    /**
     * Returns the MUT ancestor of this element, or null if no such ancestor
     * exists. Does not return itself if this element is a MUT category.
     *
     * @return The MUT ancestor
     */
    LinkedList<ModelElementContainer> getMUTAncestorPath() {
        return getMUTAncestorPath(this, new LinkedList<>());
    }

}
