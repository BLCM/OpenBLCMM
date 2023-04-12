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

import blcmm.model.properties.GlobalListOfProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The main class of the model. This class handles parsing and conversion from
 * string to model to tree and vice versa
 *
 * @author LightChaosman
 */
public class Category extends ModelElementContainer<ModelElement> {

    public static final String DEFAULT_ROOT_NAME = "root";

    private String name;
    private boolean mutuallyExclusive = false;
    private boolean locked = false;

    /**
     * Constructs a category with the given name and parent. Defaults to a
     * selected non-mutually exclusive category
     *
     * @param name The name of the category
     */
    public Category(String name) {
        this(name, false, false);
    }

    /**
     * Constructs a category with the given name and parent and MUT/locked
     * properties.
     *
     * @param name The name of the category
     * @param mutuallyExclusive indicates if this is a mutually exclusive
     * category
     * @param locked indicates if this is a locked category
     */
    public Category(String name, boolean mutuallyExclusive, boolean locked) {
        this.name = name;
        this.mutuallyExclusive = mutuallyExclusive;
        this.locked = locked;
        this.transientData = new TransientModelData(this);
    }

    public String getName() {
        return name;
    }

    /**
     *
     * @return true iff only one of the children of this category should be
     * selected at any given time
     */
    public boolean isMutuallyExclusive() {
        return mutuallyExclusive;
    }

    public void setMutuallyExclusive(boolean mutuallyExclusive) {
        this.mutuallyExclusive = mutuallyExclusive;
        transientData.updateByChangingOwnProperty(GlobalListOfProperties.CLASS_TO_INSTANCE_MAP.get(GlobalListOfProperties.MUTChecker.class).get(0));
    }

    /**
     * Changes the name of this Category
     *
     * @param name The new name for this Category
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void sort() {
        combineHotfixWrappers();
        super.sort();
        for (ModelElement el : getElements()) {
            if (el instanceof HotfixWrapper) {
                ((HotfixWrapper) el).sort();
            }
        }
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * Returns a list of all hotfixwrappers contained in this category and its
     * subcategories
     *
     * @return
     */
    List<HotfixWrapper> listHotfixMeta() {
        ArrayList<HotfixWrapper> list = new ArrayList<>();
        for (ModelElement c : getElements()) {
            if (c instanceof HotfixWrapper) {
                list.add((HotfixWrapper) c);
            } else if (c instanceof Category) {
                list.addAll(((Category) c).listHotfixMeta());
            }
        }
        return list;
    }

    @Override
    public String toString() {
        if (this.getTransientData() == null || true /**/) {
            return name;
        } else {
            return name + " " + this.getTransientData().summaryString();
        }
    }

    @Override
    String getXMLStringPrefix() {
        return String.format("<category name=\"%s\"%s%s>", name.replace("\"", "\\\""), mutuallyExclusive ? " MUT=\"true\"" : "", locked ? " locked=\"true\"" : "");
    }

    @Override
    String getXMLStringPostfix() {
        return "</category>";
    }

    @Override
    public Category copy() {
        Category copy = new Category(name, mutuallyExclusive, locked);
        for (ModelElement c : getElements()) {
            ModelElement c2 = c.copy();
            c2.setParent(copy);
            copy.addElement(c2);
        }
        return copy;
    }

    private void combineHotfixWrappers() {
        HashMap<String, HotfixWrapper> combiner = new HashMap<>();
        HashSet<HotfixWrapper> toRemove = new HashSet<>();
        for (ModelElement e : getElements()) {
            if (e instanceof HotfixWrapper) {
                HotfixWrapper wrap = (HotfixWrapper) e;
                String uniqid = wrap.getXMLStringPrefix();
                if (combiner.containsKey(uniqid)) {
                    HotfixWrapper newp = combiner.get(uniqid);
                    wrap.reverse();//since we extract in reverse order to maintain O(n), we need to reverse the list.
                    while (wrap.getElements().size() > 0) {
                        HotfixCommand s = wrap.removeElement(wrap.getElements().size() - 1);
                        s.setParent(newp);
                        newp.addElement(s);
                    }
                    toRemove.add(wrap);
                } else {
                    combiner.put(uniqid, wrap);
                }
            }
        }
        for (HotfixWrapper wrap : toRemove) {
            removeElement(wrap);
        }
    }

    void combineAdjecantHotfixWrappers() {
        HashSet<HotfixWrapper> toRemove = new HashSet<>();
        HotfixWrapper last = null;
        for (int i = 0; i < getElements().size(); i++) {
            ModelElement e = get(i);
            if (e instanceof HotfixWrapper) {
                HotfixWrapper wrap = (HotfixWrapper) e;
                if (wrap.size() == 0) {
                    toRemove.add(wrap);
                    continue;
                }
                if (last == null) {
                    last = wrap;
                } else if (wrap.getXMLStringPrefix().equals(last.getXMLStringPrefix())) {
                    wrap.reverse();//since we extract in reverse order to maintain O(n), we need to reverse the list.
                    while (wrap.getElements().size() > 0) {
                        HotfixCommand s = wrap.removeElement(wrap.size() - 1);
                        s.setParent(last);
                        last.addElement(s);
                    }
                    toRemove.add(wrap);
                } else {
                    last = wrap;
                }
            } else {
                last = null;
            }
        }
        for (HotfixWrapper wrap : toRemove) {
            removeElement(wrap);
        }
    }

    /**
     * After this method is complete, the provided element will be at the given
     * index of the list that is obtained by pretending that all children of
     * hotfixwrappers are children of this category, just like how the
     * checkboxtree displays it.
     *
     * @param el
     * @param index
     */
    void addElementAtIndexIncludingWrappers(ModelElement el, int index) {
        assert !(el instanceof HotfixCommand);
        int idx = 0;
        boolean inserted = false;
        if (index == 0) {
            inserted = true;
            addElement(el, 0);
        }
        for (int i = 0; i < size() && !inserted; i++) {
            ModelElement el2 = get(i);
            if (el2 instanceof HotfixWrapper) {
                idx += ((HotfixWrapper) el2).size();
            } else {
                idx++;
            }
            if (idx == index) {//a regular insert at i will do
                inserted = true;
                addElement(el, i + 1);
            } else if (idx > index) {
                //el2 is a hotfixwrapper with more than 1 element, and we need to insert it midway trough
                inserted = true;
                HotfixWrapper oldwrap = (HotfixWrapper) el2;
                HotfixWrapper newwrap = new HotfixWrapper(oldwrap.getName(), oldwrap.getType(), oldwrap.getParameter());
                newwrap.setParent(this);
                addElement(newwrap, i + 1);
                addElement(el, i + 1);//new element goes in front of the new wrapper. Now we move elements from the old wrapper to the new wrapper to properly place the new element
                List<HotfixCommand> reversedTail = new ArrayList<>();
                for (int j = 0; j < (idx - index); j++) {
                    reversedTail.add(oldwrap.removeElement(oldwrap.size() - 1));
                }
                for (int j = reversedTail.size() - 1; j >= 0; j--) {
                    HotfixCommand s = reversedTail.get(j);
                    s.setParent(newwrap);
                    newwrap.addElement(s);
                }
            }
        }
        if (!inserted) {
            addElement(el);
        }
        combineAdjecantHotfixWrappers();
    }

    public int sizeIncludingHotfixes() {
        int size = 0;
        for (ModelElement e : getElements()) {
            if (e instanceof HotfixWrapper) {
                size += ((HotfixWrapper) e).size();
            } else {
                size++;
            }
        }
        return size;
    }

    int indexOfIncludingHotfixes(ModelElement el) {
        int size = 0;
        for (ModelElement e : getElements()) {
            if (e == el) {
                return size;
            } else if (e instanceof HotfixWrapper) {
                if (el instanceof SetCommand) {
                    for (SetCommand s : ((HotfixWrapper) e).getElements()) {
                        if (s == el) {
                            return size;
                        }
                        size++;
                    }
                } else {
                    size += ((HotfixWrapper) e).size();
                }
            } else {
                size++;
            }
        }
        return -1;
    }

    void clear() {
        while (getElements().size() > 0) {
            ModelElement removeElement = removeElement(getElements().size() - 1);
            removeElement.setParent(null);
        }
    }
}
