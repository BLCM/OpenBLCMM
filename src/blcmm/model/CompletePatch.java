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
import blcmm.utilities.GlobalLogger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author LightChaosman
 */
public class CompletePatch {

    private Category root;
    final LinkedHashMap<String, Profile> profiles;
    private PatchType type;
    private Profile currentProfile;
    private boolean offline;

    /**
     * This is more like metadata, but doesn't really make sense anywhere else.
     */
    public static enum PatchSource {
        UNSPECIFIED,
        BLCMM,
        FT,
        HOTFIX,
    }
    private PatchSource patchSource;

    public CompletePatch() {
        profiles = new LinkedHashMap<>();
        type = PatchType.BL2;
        offline = false;
        patchSource = PatchSource.UNSPECIFIED;
    }

    public Profile getCurrentProfile() {
        return currentProfile;
    }

    public void setOffline(boolean aFlag) {
        this.offline = aFlag;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setPatchSource(PatchSource newSource) {
        this.patchSource = newSource;
    }

    public PatchSource getPatchSource() {
        return this.patchSource;
    }

    public Category getRoot() {
        return root;
    }

    public PatchType getType() {
        return type;
    }

    public void setType(PatchType type) {
        this.type = type;
    }

    public void setRoot(Category root) {
        this.root = root;
    }

    String getprofileXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\t<profiles>\n");
        for (Profile prof : profiles.values()) {
            String s = prof.toString();
            if (prof == currentProfile) {
                s = s.replace("/>", " current=\"true\"/>");
            }
            sb.append("\t\t\t").append(s).append("\n");
        }
        sb.append("\t\t</profiles>\n");
        return sb.toString();
    }

    /**
     * Returns the profile with the given name, or null if no such profile
     * exists.
     *
     * @param name
     * @return
     */
    public Profile getProfile(String name) {
        return profiles.get(name);
    }

    public Collection<Profile> getProfiles() {
        return profiles.values();
    }

    public void renameProfile(Profile profile, String name) {
        profiles.remove(profile.getName());
        profiles.put(name, profile);
        profile.setName(name);
    }

    public Profile createNewProfile(String name) {
        Profile prof = new Profile(name);
        profiles.put(name, prof);
        if (root != null) {
            for (ModelElement c : root.listRecursiveContentMinusCategories()) {
                if (c instanceof EnableableModelElement && ((EnableableModelElement) c).isSelected()) {
                    ((SetCommand) c).turnOnInProfile(prof);
                }
            }
        }
        setCurrentProfile(prof);
        GlobalLogger.log("Profile Editor - created profile " + prof.getName());
        return prof;
    }

    public void setCurrentProfile(String newprofile) {
        setCurrentProfile(getProfile(newprofile));
    }

    public void setCurrentProfile(Profile prof) {
        if (prof == null) {
            throw new NullPointerException();
        }
        this.currentProfile = prof;
        if (root != null) {
            for (ModelElement c : root.listRecursiveContentMinusCategories()) {
                if (c instanceof EnableableModelElement) {
                    ((EnableableModelElement) c).profileChanged(prof);
                }
            }
        }
    }

    public void deleteProfile(Profile prof) {
        if (prof == currentProfile) {
            if (profiles.size() == 1) {
                throw new IllegalArgumentException("can not delete last profile");
            } else {
                int idx = 0;
                Profile prev = null;
                boolean next = false;
                for (Profile p : profiles.values()) {
                    if (p == prof && idx == 0) {
                        next = true;
                    } else if (p == prof && idx > 0) {
                        currentProfile = prev;
                        break;
                    } else if (next) {
                        currentProfile = p;
                        break;
                    }
                    prev = p;
                    idx++;
                }
            }
        }
        profiles.remove(prof.getName());
        if (root != null) {
            for (ModelElement c : root.listRecursiveContentMinusCategories()) {
                if (c instanceof EnableableModelElement) {
                    ((EnableableModelElement) c).turnOffInProfile(prof);
                    ((EnableableModelElement) c).profileChanged(currentProfile);
                }
            }
        }
        GlobalLogger.log("Profile Editor - deleted profile " + prof.getName());
    }

    public Category introduceCategoryAsParentOfElements(ArrayList<ModelElement> elements, String categoryName) {
        if (elements.isEmpty()) {
            throw new NullPointerException("Cannot process an empty list of elements");
        }
        ModelElement first = elements.get(0);
        if (first.getParent() == null) {
            throw new NullPointerException("Can not create a new root");
        }
        Category parentOfNewCategory;
        if (first.getParent() instanceof HotfixWrapper) {
            parentOfNewCategory = first.getParent().getParent();
        } else {
            parentOfNewCategory = (Category) first.getParent();
        }
        Category cat = new Category(categoryName);
        int index = parentOfNewCategory.indexOfIncludingHotfixes(first);
        for (ModelElement el : elements) {
            insertElementInto(el, cat);
        }
        insertElementInto(cat, parentOfNewCategory, index);
        return cat;
    }

    public void insertElementInto(ModelElement el, Category newParent) {
        insertElementInto(el, newParent, newParent.sizeIncludingHotfixes());
    }

    public void insertElementInto(ModelElement el, Category newParent, int index) {
        int size = newParent.sizeIncludingHotfixes();
        int sizeOfChild = ((el instanceof HotfixWrapper) ? ((HotfixWrapper) el).size() : 1);
        ModelElement el2;
        if (el.getParent() instanceof HotfixWrapper) {
            HotfixWrapper oldwrap = (HotfixWrapper) el.getParent();
            el2 = new HotfixWrapper(oldwrap.getName(), oldwrap.getType(), oldwrap.getParameter());
            removeElementFromParentCategory(el);
            el.setParent((HotfixWrapper) el2);
            ((HotfixWrapper) el2).addElement((HotfixCommand) el);
        } else {
            el2 = el;
            removeElementFromParentCategory(el);
        }
        el2.setParent(newParent);
        newParent.addElementAtIndexIncludingWrappers(el2, index);
        assert newParent.sizeIncludingHotfixes() == size + sizeOfChild : "Size was " + size + " before inserting a child of size " + sizeOfChild + " and now it is " + newParent.sizeIncludingHotfixes();
    }

    public boolean removeElementFromParentCategory(ModelElement modelElement) {
        ModelElementContainer modelparent = modelElement.getParent();
        if (modelparent == null) {
            return false;
        }
        boolean a = modelparent.removeElement(modelElement);
        modelElement.setParent(null);
        if (modelparent instanceof HotfixWrapper && modelparent.size() == 0) {
            modelparent.getParent().removeElement(modelparent);//get rid of empty hotfixwrappers
        }
        return a;
    }

    public void setSelected(SetCommand com, boolean check) {
        if (check) {
            if (1 == 0) { //MUT enforcement on the model level
                //selecting things like this is only done trough the GUI.
                //The GUI has its own MUT enforcing mechanisims, which are far more efficient than this one
                //We choose to leave this piece of code unused, trusting in our GUI code.
                //TODO adding a TODO  marker to make sure this descision is not forgotten.
                LinkedList<ModelElementContainer> mutAncestorPath = com.getMUTAncestorPath();
                if (mutAncestorPath != null) {//We have a mut ancestor, stored at the end of the list above
                    Category mut = (Category) mutAncestorPath.getLast();
                    Category childOfMUT = (Category) mutAncestorPath.get(mutAncestorPath.size() - 1);
                    if (mut.getTransientData().getNumberOfOccurences(GlobalListOfProperties.LeafSelectedChecker.class) == 0) {
                        //no children of the mut category are selected, so we're good
                    } else {
                        for (ModelElement el : mut.getElements()) {
                            if (el != childOfMUT
                                    && el.getTransientData().getNumberOfOccurences(GlobalListOfProperties.LeafSelectedChecker.class) > 0) {
                                //MUT violated
                                throw new IllegalStateException("MUT property violated");
                            }
                        }
                        //reached the end of the loop?
                        //All selected elements were in the child of mut containing this element.
                    }
                }
            }
            com.turnOnInProfile(getCurrentProfile());
        } else {
            com.turnOffInProfile(getCurrentProfile());
        }
        com.profileChanged(getCurrentProfile());
    }

    public void deleteAllProfilesAndReplaceCurrentProfileWith(Profile currentProfile) {
        profiles.clear();
        profiles.put(currentProfile.getName(), currentProfile);
        for (ModelElement el : root.listRecursiveContentMinusCategories()) {
            if (el instanceof EnableableModelElement) {
                EnableableModelElement set = (EnableableModelElement) el;
                for (Profile p : set.getProfiles().toArray(new Profile[0])) {
                    set.turnOffInProfile(p);
                }
                if (set.isSelected()) {
                    set.turnOnInProfile(currentProfile);
                }
            }
        }
    }

    void fixInvalidMUT() {
        //Define our one-time use function
        Runnable checker = () -> {
            List<Category> mutViolators = verifyMUT();
            for (Category c : mutViolators) {
                for (ModelElement el : c.listRecursiveContentMinusCategories()) {
                    if (el instanceof EnableableModelElement) {
                        ((EnableableModelElement) el).turnOffInProfile(getCurrentProfile());
                    }
                }
            }
        };
        Profile prof = getCurrentProfile();
        checker.run();
        for (Profile p : getProfiles()) {
            if (p == prof) {
                continue;
            }
            setCurrentProfile(p);
            checker.run();
        }
        if (getProfiles().size() > 1) {
            setCurrentProfile(prof);
        }
    }

    /**
     * Returns a list containing all the categories that are currently violating
     * the MUT property
     *
     * @return
     */
    private List<Category> verifyMUT() {
        return verifyMUT(getRoot(), new ArrayList<>());
    }

    private List<Category> verifyMUT(Category root, List<Category> results) {
        if (root.isMutuallyExclusive()) {
            boolean prev = false;
            for (ModelElement el : root.getElements()) {
                int selected = el.getTransientData().getNumberOfOccurences(GlobalListOfProperties.LeafSelectedChecker.class);
                if (selected > 0 && prev) {
                    results.add(root);
                    break;
                }
                prev = prev || selected > 0;
            }
        }
        for (ModelElement el : root.getElements()) {
            if (el instanceof Category) {
                verifyMUT((Category) el, results);
            }
        }
        return results;
    }

    public void deselectAll() {
        deselectEntireCategory(getRoot());
    }

    public void deselectEntireCategory(Category fromCategory) {
        for (ModelElement el : fromCategory.listRecursiveContentMinusCategories()) {
            if (el instanceof SetCommand) {
                setSelected((SetCommand) el, false);
            }
        }
    }

}
