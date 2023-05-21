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
package blcmm.gui.tree;

import blcmm.gui.theme.ThemeManager;
import blcmm.model.Category;
import blcmm.model.HotfixType;
import blcmm.model.HotfixWrapper;
import blcmm.model.ModelElement;
import blcmm.model.ModelElementContainer;
import blcmm.model.SetCommand;
import blcmm.model.TransientModelData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class to handle looking for overwritten statements in our tree, and colorize
 * them appropriately.
 *
 * TODO:  This class does not currently handle the following case in TPS
 * (assuming both statements are hotfixes):
 *
 *     set foo bar +(baz)
 *     set foo bar (frotz)
 *
 * The array-add syntax from TPS is basically completely ignored, since we
 * discard all instances of it while constructing.
 * https://github.com/BLCM/OpenBLCMM/issues/20
 *
 * @author LightChaosman
 */
public class OverwriteChecker {

    /**
     * The single instance of this class
     */
    private static final OverwriteChecker INSTANCE = new OverwriteChecker();

    public static List<SetCommand> getCompleteOverwriters(SetCommand command) {
        return getList(command, true, false);
    }

    public static List<SetCommand> getPartialOverwriters(SetCommand command) {
        return getList(command, true, true);
    }

    public static List<SetCommand> getCompleteOverwrittens(SetCommand command) {
        return getList(command, false, false);
    }

    public static List<SetCommand> getPartialOverwrittens(SetCommand command) {
        return getList(command, false, true);
    }

    private static List<SetCommand> getList(SetCommand command, boolean overwritesMe, boolean partial) {
        TreeSet<SetCommandPlus> res1 = new TreeSet<>();
        String start = Helpers.getStart(command);
        boolean subPrefixUseful = partial != overwritesMe;
        SortedMap<String, TreeSet<SetCommandPlus>> elementsWithPrefix
                = Helpers.getElementsWithPrefix(
                        INSTANCE.overwriteMap,
                        subPrefixUseful ? Helpers.getHotfixFreeStart(command) : start);
        SetCommandPlus thisCommand = null;
        if (elementsWithPrefix.containsKey(start)) {
            // AFAIK, this should never *not* be reached in ordinary operation.  We're doing the containsKey
            // check basically entirely for the benefit of our unit tests, because we'll end up calling this
            // with TPS-style +(array) additions, which won't be in our structures.
            for (SetCommandPlus s : elementsWithPrefix.get(start)) {
                if (s.command == command) {
                    thisCommand = s;//This could be done with a hashmap or so, but since this loop will usually only be a very small number of elements, this is fine.
                    break;
                }
            }
        }
        if (thisCommand == null) {
            // AFAIK, this should never be reached in ordinary operation, 'cause the GUI won't even give
            // the user an option to get in here.  However, our unit tests for this stuff will end up
            // calling in here with TPS-style +(array) additions, which won't be anywhere in overwriteMap,
            // so just handle it regardless.
            return new ArrayList<>();
        }
        for (String prefix : elementsWithPrefix.keySet()) {
            if (1 == 0) { //Two ways of doing the same thing... I think, the 'false' branch is more effiecient about it.
                if (false //below is the super verbose way of doing things, with multiple startsWith and equals evaluations
                        || ((overwritesMe && !partial) && (start.startsWith(prefix)))
                        || ((overwritesMe && partial) && (prefix.startsWith(start) && !start.equals(prefix)))
                        || ((!overwritesMe && !partial) && (prefix.startsWith(start)))
                        || ((!overwritesMe && partial) && (start.startsWith(prefix) && !start.equals(prefix)))) {
                    if (!isValidSuperString(start, prefix)) {
                        continue;
                    }
                    TreeSet<SetCommandPlus> toAdd1 = elementsWithPrefix.get(prefix);
                    Set<SetCommandPlus> toAdd2 = overwritesMe ? toAdd1.tailSet(thisCommand, false) : toAdd1.headSet(thisCommand, false);
                    res1.addAll(toAdd2);
                }
            } else {
                if (true //This is the compact & effient way, probably effectively the same as above.
                        && !(start.equals(prefix) && partial)//If we're looking at partial stuff, we exclude our own prefix
                        && (subPrefixUseful ? start.startsWith(prefix) : true)//
                        //used to be: subPrefixUseful ? start.startsWith(prefix) : prefix.startsWith(start)
                        //But prefix.startsWith(start) is always true when subPrefixUseful is true, because of the condition at the top
                        ) {
                    if (!isValidSuperString(start, prefix)) {
                        continue;
                    }
                    TreeSet<SetCommandPlus> toAdd1 = elementsWithPrefix.get(prefix);
                    Set<SetCommandPlus> toAdd2 = overwritesMe ? toAdd1.tailSet(thisCommand, false) : toAdd1.headSet(thisCommand, false);
                    res1.addAll(toAdd2);
                }
            }
        }

        assert thisCommand != null;
        List<SetCommand> res = new ArrayList<>();
        for (SetCommandPlus s : res1) {
            res.add(s.command);
        }
        return res;
    }

    private static boolean isValidSuperString(String prefix, String superString) {
        if (prefix.length() == superString.length()) {
            return true;
        }
        if (superString.length() < prefix.length()) {
            return isValidSuperString(superString, prefix);
        }
        char last = prefix.charAt(prefix.length() - 1);
        if (superString.charAt(prefix.length()) == '.') {
            return true;
        } else {
            int x = prefix.length();
            if (superString.charAt(x) != '[') {
                return false;
            }
            x++;
            while (x < superString.length() && Character.isDigit(superString.charAt(x))) {
                x++;
            }
            if (x == superString.length()) {
                return false;
            }
            if (superString.charAt(x) != ']') {
                return false;
            }
            return true;
        }
    }

    /**
     * Returns the ColorType of the given element
     *
     * @param el The element of which to retrieve the color
     * @return
     */
    public static ThemeManager.ColorType getColor(ModelElement el) {
        return INSTANCE.colorTypeMap.get(el);
    }

    /**
     * Resets ourselves and kicks off a full scan of the tree, or at least a
     * scan starting at the passed-in "root."
     *
     * @param root The category at which to start re-scanning
     */
    public static void reset(Category root) {
        INSTANCE.colorTypeMap.clear();
        INSTANCE.overwriteMap.clear();
        INSTANCE.scan(root, false);
        INSTANCE.scan(root, true);
    }
    /**
     * The structure we use to store our overwrite statuses
     */
    private final TreeMap<String, TreeSet<SetCommandPlus>> overwriteMap = new TreeMap<>();

    /**
     * A HashMap to keep track of what ColorType to use. This lets us change the
     * color of the highlighting dynamically as the theme is changed.
     */
    private final HashMap<ModelElement, ThemeManager.ColorType> colorTypeMap = new HashMap<>();

    /**
     * The single, private, constructor, that is called a grand total of 1 time.
     */
    private OverwriteChecker() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
    }

    /**
     * Scans the given element -- if it is a container, we will recurse into it,
     * otherwise we'll analyze it as a leaf node.
     *
     * @param element The element to scan
     * @return True if this element or any of its children are overwriters.
     */
    private boolean scan(ModelElement element, final boolean hotfixes) {
        if (element instanceof ModelElementContainer) {
            preOrder(element, hotfixes);
            if (((ModelElementContainer) element).getElements().isEmpty()) {
                return analyzeLeaf(element, true);
            } else if (true) {//Since the root and 'mods' don't accept transient data... we can't be smart here, yet
                boolean isOverwriting = false;
                for (ModelElement el : ((ModelElementContainer<ModelElement>) element).getElements()) {
                    isOverwriting = combine(scan(el, hotfixes), isOverwriting);
                }
                postOrder(element, isOverwriting);
                return isOverwriting;
            } else {//If nothing inside us is selected, don't even bother scanning
                return false;
            }
        } else {
            //leaf
            return analyzeLeaf(element, hotfixes);
        }
    }

    /**
     * This is called when first processing a category
     *
     * @param element The element to process
     */
    private void preOrder(ModelElement element, boolean secondRun) {
        if (!secondRun) {
            colorTypeMap.remove(element);
            element.getTransientData().setOverwriteState(TransientModelData.OverwriteState.Normal);
        }
    }

    /**
     * This is called when we have finished processing a category.
     *
     * @param element The element we've just finished processing
     * @param isOverwriting Whether or not this element (or its contents) is
     * overwriting
     */
    private void postOrder(ModelElement element, boolean isOverwriting) {
        // If we're overwriting but we're not marked as overwriting yet,
        // do so.
        if (isOverwriting && !colorTypeMap.containsKey(element)) {
            this.setOverwriter(element);
        }
    }

    /**
     * Convenience function to combine the results of two scans/results. This is
     * just doing a logical OR on the two arguments, but it makes the code look
     * a bit cleaner.
     *
     * @param result One result to compare
     * @param newResult The other result to compare
     * @return True of either result or newResult are True, False if they are
     * both false
     */
    private boolean combine(boolean result, boolean newResult) {
        return result || newResult;
    }

    /**
     * Analyze a leaf element. Should nearly always be a SetCommand, but could
     * technically be a Category.
     *
     * @param element The element to investigate
     * @return True if we were set as an overwriter, False otherwise.
     */
    private boolean analyzeLeaf(ModelElement element, final boolean hotfixesOnly) {
        if (!hotfixesOnly) {//first scan
            element.getTransientData().setOverwriteState(TransientModelData.OverwriteState.Normal);
        }
        if (element instanceof SetCommand) {
            if (!((SetCommand) element).isSelected()) {
                return false;
            }
            if (((SetCommand) element).getValue().startsWith("+(")) {
                return false;
            }
            if (((SetCommand) element).getField().equalsIgnoreCase("levellist")) {
                return false;
            }
            if ((element.getParent() instanceof HotfixWrapper) != hotfixesOnly) {//Note that we are guaranteed to have a parent, so no nullpointer can be thrown
                return false;
            }
            SetCommand setCommand = (SetCommand) element;
            String start = Helpers.getStart(setCommand);
            String noHotfixPrefix = Helpers.getHotfixFreeStart(setCommand);
            SortedMap<String, TreeSet<SetCommandPlus>> elementsWithPrefix = Helpers.getElementsWithPrefix(overwriteMap, noHotfixPrefix);
            SortedMap<String, TreeSet<SetCommandPlus>> elementsWithFullPrefix = Helpers.getElementsWithPrefix(overwriteMap, start);
            int p_overwrites = 0;
            for (String prefix : elementsWithPrefix.keySet()) {
                if (prefix.startsWith(start)) {//We completely overwrite these, so these are handled below
                    continue;
                } else if (!start.startsWith(prefix)) {//This hotfix modifies a non-overlapping part from the ones starting with the current prefix
                    continue;
                }
                p_overwrites++;
                //ModelElement el = elementsWithPrefix.get(prefix).last().command;
                //setPartialOverwritten(el);//We don't color partially overwritten elements, only partial overwriters
            }
            if (p_overwrites > 0) {
                setPartialOverwriter(element);
            }
            boolean overwrite = false;
            if (!elementsWithFullPrefix.isEmpty()) {
                int count = 0;
                for (String prefix : elementsWithFullPrefix.keySet()) {
                    if (!isValidSuperString(prefix, start)) {
                        continue;
                    }
                    ModelElement el = elementsWithFullPrefix.get(prefix).last().command;
                    count++;
                    setOverwritten(el);
                    while ((el = el.getParent()) != null) {
                        setOverwritten(el);
                    }
                }
                if (count > 0) {
                    setOverwriter(element);
                    overwrite = true;
                }
            }
            overwriteMap.putIfAbsent(start, new TreeSet<>());
            overwriteMap.get(start).add(new SetCommandPlus(setCommand));
            return overwrite;
        }
        return false;
    }

    /**
     * Convenience function to set overwrite status to "Overwriter," for an
     * element.
     *
     * @param element The element to set
     */
    private void setOverwriter(ModelElement element) {
        this.setOverwriteState(element, ThemeManager.ColorType.TreeOverwriterChecker, TransientModelData.OverwriteState.Overwriter);
    }

    /**
     * Convenience function to set overwrite status to "Overwritten," for an
     * element.
     *
     * @param element The element to set
     */
    private void setOverwritten(ModelElement element) {
        this.setOverwriteState(element, ThemeManager.ColorType.TreeOverwrittenChecker, TransientModelData.OverwriteState.Overwritten);
    }

    /**
     * Convenience function to set overwrite status to "PartialOverwritten," for
     * an element.
     *
     * @param element The element to set
     */
    private void setPartialOverwritten(ModelElement element) {
        this.setOverwriteState(element, ThemeManager.ColorType.TreePartialOverwrittenChecker, TransientModelData.OverwriteState.PartialOverwritten);
    }

    /**
     * Convenience function to set overwrite status to "PartialOverwritten," for
     * an element.
     *
     * @param element The element to set
     */
    private void setPartialOverwriter(ModelElement element) {
        this.setOverwriteState(element, ThemeManager.ColorType.TreePartialOverwriterChecker, TransientModelData.OverwriteState.PartialOverwriter);
    }

    /**
     * Convenience function to set overwrite status on an element.
     *
     * @param element The element to set
     * @param colorType The ThemeManager.ColorType constant to use for color
     * @param overwriteState The state to set for the given element
     */
    private void setOverwriteState(ModelElement element, ThemeManager.ColorType colorType, TransientModelData.OverwriteState overwriteState) {
        if (element.getTransientData().setOverwriteState(overwriteState)) {
            colorTypeMap.put(element, colorType);
        }
    }

    private static final class SetCommandPlus implements Comparable<SetCommandPlus> {

        private static int COUNTER = 0;

        private final SetCommand command;
        private final int idx = COUNTER++;

        SetCommandPlus(SetCommand command) {
            this.command = command;
        }

        @Override
        public int compareTo(SetCommandPlus t) {
            return idx - t.idx;
        }

    }

    private final static class Helpers {

        /**
         * Retrieves the start of a set command, to keep track of what's been
         * overwritten. Will strip out the "set" or "set_cmp" initial token so
         * that only the object and attribute are being considered.
         *
         * @param setCommand The command to process
         * @return A string containing the object and attribute being modified
         */
        private static String getStart(SetCommand setCommand) {
            String object = getStrippedObject(setCommand) + " ";
            String field = setCommand.getField().toLowerCase();
            if (setCommand.getParent() != null && setCommand.getParent() instanceof HotfixWrapper) {
                HotfixWrapper parent = (HotfixWrapper) setCommand.getParent();
                if (parent.getType() != HotfixType.PATCH && !"none".equalsIgnoreCase(parent.getParameter())) {
                    int dotIndex = field.indexOf('.');
                    if (dotIndex == -1) {
                        dotIndex = Integer.MAX_VALUE;
                    }
                    int bracketIndex = field.indexOf('[');
                    if (bracketIndex == -1) {
                        bracketIndex = Integer.MAX_VALUE;
                    }
                    int breakIndex = Math.min(dotIndex, bracketIndex);
                    if (breakIndex == Integer.MAX_VALUE) {
                        breakIndex = field.length();
                    }
                    String head = field.substring(0, breakIndex);
                    String tail = field.substring(breakIndex, field.length());
                    field = head + "." + parent.getParameter().toLowerCase() + tail;
                }
            }
            return object + field;
        }

        private static String getHotfixFreeStart(SetCommand setCommand) {
            String start = setCommand.getField();
            int min = start.length();
            int idx1 = start.indexOf(".");
            int idx2 = start.indexOf("[");
            if (idx1 != -1) {
                min = idx1;
            }
            if (idx2 != -1) {
                min = Math.min(min, idx2);
            }
            return getStrippedObject(setCommand) + " " + start.substring(0, min).toLowerCase();
        }

        private static String getStrippedObject(SetCommand setCommand) {
            String base = setCommand.getObject().toLowerCase();
            int idx = base.indexOf("'");
            if (idx == -1) {
                return base;
            }
            int idx2 = base.indexOf("'", idx + 1);
            if (idx2 == -1) {
                return base;
            }
            return base.substring(idx + 1, idx2);
        }

        /**
         * @param allElements - a SortedSet of strings. This set must use the
         * natural string ordering; otherwise this method may not behave as
         * intended.
         * @param prefix
         * @return The subset of allElements containing the strings that start
         * with prefix.
         */
        private static <V> SortedMap<String, V> getElementsWithPrefix(
                final SortedMap<String, V> allElements, final String prefix) {

            final Optional<String> endpoint = incrementPrefix(prefix);

            if (endpoint.isPresent()) {
                return allElements.subMap(prefix, endpoint.get());
            } else {
                return allElements.tailMap(prefix);
            }
        }

        /**
         * @param prefix
         * @return The least string that's greater than all strings starting
         * with prefix, if one exists. Otherwise, returns Optional.empty().
         * (Specifically, returns Optional.empty() if the prefix is the empty
         * string, or is just a sequence of Character.MAX_VALUE-s.)
         */
        static Optional<String> incrementPrefix(final String prefix) {
            final StringBuilder sb = new StringBuilder(prefix);

            // remove any trailing occurrences of Character.MAX_VALUE:
            while (sb.length() > 0 && sb.charAt(sb.length() - 1) == Character.MAX_VALUE) {
                sb.setLength(sb.length() - 1);
            }

            // if the prefix is empty, then there's no upper bound:
            if (sb.length() == 0) {
                return Optional.empty();
            }

            // otherwise, increment the last character and return the result:
            sb.setCharAt(sb.length() - 1, (char) (sb.charAt(sb.length() - 1) + 1));
            return Optional.of(sb.toString());
        }

    }

}
