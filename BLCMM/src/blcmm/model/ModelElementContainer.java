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
 */
package blcmm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author LightChaosman
 * @param <T> The element we're containing.
 */
public abstract class ModelElementContainer<T extends ModelElement> extends ModelElement {

    private final List<T> elements = new ArrayList<>();
    private final transient int[] longest = new int[5];
    private transient int numberOfLeafDescendants = 0, numberOfCommandsDescendants = 0, numberOfHotfixDescendants = 0;

    @Override
    public Category getParent() {
        return (Category) super.getParent();
    }

    public int size() {
        return elements.size();
    }

    public T get(int i) {
        return elements.get(i);
    }

    /**
     * set commands and hotfixes
     *
     * @return
     */
    public final int getNumberOfCommandsDescendants() {
        return numberOfCommandsDescendants;
    }

    /**
     * Comments, set commands and hotfixes
     *
     * @return
     */
    public final int getNumberOfLeafDescendants() {
        return numberOfLeafDescendants;
    }

    /**
     * Just hotfixes
     *
     * @return
     */
    public int getNumberOfHotfixDescendants() {
        return numberOfHotfixDescendants;
    }

    public List<T> getElements() {
        return Collections.unmodifiableList(elements);
    }

    void addElement(T c) {
        addElement(c, elements.size());
    }

    void addElement(T c, int i) {
        if (elements.contains(c)) {
            throw new IllegalArgumentException("Already contains argument");
        }
        if (c.getParent() != this) {
            throw new IllegalArgumentException("When adding a code, the parent must be set correctly");
        }
        if (c == this) {
            throw new IllegalArgumentException("Can't add me to myself");
        }
        if (c instanceof SetCommand) {
            updateLengths((SetCommand) c);
        } else if (c instanceof HotfixWrapper) {
            for (SetCommand s : ((HotfixWrapper) c).getElements()) {
                updateLengths(s);
            }
        }
        increaseChildCount(c);
        elements.add(i, c);
        transientData.updateByInsertingNewChild(c);
    }

    private void increaseChildCount(T t) {
        changeChildCounters(t, true);
    }

    private void decreaseChildCount(T t) {
        changeChildCounters(t, false);
    }

    private void changeChildCounters(T t, boolean add) {
        ModelElementContainer container = this;
        final int add1, add2, add3;
        int fac = add ? 1 : -1;
        if (t instanceof ModelElementContainer) {
            add1 = ((ModelElementContainer) t).numberOfCommandsDescendants;
            add2 = ((ModelElementContainer) t).numberOfLeafDescendants;
            add3 = ((ModelElementContainer) t).numberOfHotfixDescendants;
        } else if (t instanceof SetCommand) {
            add1 = 1;
            add2 = 1;
            add3 = this instanceof HotfixWrapper ? 1 : 0;
        } else if (t instanceof Comment) {
            add1 = 0;
            add2 = 1;
            add3 = 0;
        } else {
            throw new IllegalArgumentException();
        }
        while (container != null) {
            container.numberOfCommandsDescendants += (add1 * fac);
            container.numberOfLeafDescendants += (add2 * fac);
            container.numberOfHotfixDescendants += (add3 * fac);
            container = container.getParent();
        }
    }

    @Override
    void setParent(ModelElementContainer category) {
        if (category != null && !(category instanceof Category)) {
            throw new IllegalArgumentException();
        }
        super.setParent(category);
    }

    int[] getLongest() {
        return longest;
    }

    /**
     * Completely recreates our "longest" structure, recursively. This is only
     * used at the moment to recompute, when the "truncateCommands" option is
     * toggled.
     */
    public void recreateLengthsRecursive() {

        // First zero out our length
        for (int i = 0; i < longest.length; i++) {
            longest[i] = 0;
        }

        // Now loop through our elements, recursing where needed, and adding
        // in any children to our "longest" var.
        for (T element : elements) {
            if (element instanceof SetCommand || element instanceof SetCMPCommand) {
                // If we found a "set" command, update its lengths.
                // Note that if we're a HotfixWrapper, this will be propagated
                // to the parent, thanks to our overridden updateLengths().
                updateLengths((SetCommand) element);
            } else if (element instanceof ModelElementContainer) {
                // If we found a container of some sort, recurse into it.
                ((ModelElementContainer) element).recreateLengthsRecursive();
            } else if (element instanceof Comment) {
                // Do nothing, just leave these alone
            } else {
                Logger.getLogger(ModelElementContainer.class.getName()).log(
                        Level.WARNING,
                        "Unknown child while recreating command sizes: "
                        + element.getClass().toString());
            }
        }
    }

    void updateLengths(SetCommand c) {
        String[] split = c.getSplit();
        for (int i = 0; i < Math.min(longest.length, split.length); i++) {
            longest[i] = Math.max(longest[i], split[i].length());
        }
    }

    boolean removeElement(T t) {
        boolean a = elements.remove(t);
        if (!a) {
            return false;
        }
        decreaseChildCount(t);
        transientData.updateByRemovingChild(t);
        return true;
    }

    T removeElement(int i) {
        T t = elements.remove(i);
        decreaseChildCount(t);
        transientData.updateByRemovingChild(t);
        return t;
    }

    protected void reverse() {
        Collections.reverse(elements);
    }

    public void sort() {
        this.elements.sort(ModelElement.ELEMENT_COMPERATOR);
    }

    /**
     * Returns a list of all non-container content of this category.
     *
     * @return
     */
    List<ModelElement> listRecursiveContentMinusCategories() {
        ArrayList<ModelElement> cds = new ArrayList<>();
        for (T c : this.elements) {
            if (c instanceof ModelElementContainer) {
                cds.addAll(((ModelElementContainer) c).listRecursiveContentMinusCategories());
            } else {
                cds.add((ModelElement) c);
            }
        }
        return cds;
    }

    @Override
    protected final String toXMLString() {
        throw new IllegalStateException("toXMLString is not used for containers.");
    }

    abstract String getXMLStringPrefix();

    abstract String getXMLStringPostfix();

}
