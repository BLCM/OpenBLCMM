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
package blcmm.gui.tree.rightmouse;

import blcmm.data.lib.BorderlandsObject;
import blcmm.data.lib.DataManager;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class InvertModAction extends RightMouseButtonAction {

    public InvertModAction(CheckBoxTree tree) {
        super(tree, "Invert mod", new Requirements(true, true, true, false));
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return false;
        }
        return paths.length == 1 && ((DefaultMutableTreeNode) paths[0].getLastPathComponent()).getUserObject() instanceof Category;
    }

    int commands = 0;
    int inverted = 0;

    @Override
    public void action() {

        DataManager.getDictionary().getElementsInClassWithPrefix("SkillDefinition", "adssada");
        TreePath[] paths = tree.getSelectionPaths();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        Category toBeInverted = (Category) node.getUserObject();
        CompletePatch patch = tree.getPatch();
        Category rootOfInverted = new Category(toBeInverted.getName() + "'s inversion");
        Category uninv = new Category("Could not be inverted");
        Category inv = new Category("Sucesfully inverted");
        patch.insertElementInto(uninv, rootOfInverted);
        patch.insertElementInto(inv, rootOfInverted);
        commands = toBeInverted.getNumberOfCommandsDescendants();
        inverted = 0;
        invertInto(toBeInverted, inv, uninv, patch);
        inv.sort();
        uninv.sort();
        patch.insertElementInto(rootOfInverted, toBeInverted);
        if (uninv.size() == 0) {
            patch.removeElementFromParentCategory(uninv);
        }
        if (inv.size() == 0) {
            patch.removeElementFromParentCategory(inv);
        }
        DefaultMutableTreeNode node2 = CheckBoxTree.createTree(rootOfInverted);
        tree.getModel().insertNodeInto(node2, node, node.getChildCount());
        tree.setChanged(true);
    }

    private void invertInto(Category toBeInverted, Category inverted, Category uninverted, CompletePatch patch) {
        long l = System.currentTimeMillis();
        HashMap<String, HashSet<String>> classToObjectsMap = new HashMap<>();
        HashMap<String, HashSet<SetCommand>> objectToCommandsMap = new HashMap<>();
        Collection<SetCommand> codes = new ArrayList<>();
        Collection<HotfixWrapper> wrappers = new ArrayList<>();
        extractData(toBeInverted, codes, wrappers);
        for (SetCommand s : codes) {
            analyzeCommand(s, classToObjectsMap, objectToCommandsMap);
        }
        for (HotfixWrapper wrap : wrappers) {
            for (SetCommand s : wrap.getElements()) {
                analyzeCommand(s, classToObjectsMap, objectToCommandsMap);
            }
        }
        for (String clazz : classToObjectsMap.keySet()) {
            HashSet<String> objects = classToObjectsMap.get(clazz);
            if (clazz == null) {
                for (String object : objects) {
                    for (SetCommand com : objectToCommandsMap.get(object)) {
                        ModelElement el;
                        if (com.getParent() instanceof HotfixWrapper) {
                            HotfixWrapper oldw = (HotfixWrapper) com.getParent();
                            el = new HotfixWrapper(oldw.getName(), oldw.getType(), oldw.getParameter(), Arrays.asList(new String[]{com.getCode()}));
                        } else {
                            el = new SetCommand(com.getCode());
                        }
                        patch.insertElementInto(el, uninverted);
                    }
                }
            } else {
                DataManager.streamAllDumpsOfClass(clazz).filter(d -> objects.contains(d.object)).forEach(d -> {
                    String[] cfields = new String[objectToCommandsMap.get(d.object).size()];
                    String[] fields = new String[cfields.length];
                    SetCommand[] coms = new SetCommand[cfields.length];
                    int k = 0;
                    for (SetCommand com : objectToCommandsMap.get(d.object)) {
                        coms[k] = com;
                        cfields[k] = coms[k].getField();
                        fields[k] = cfields[k];
                        if (fields[k].contains(".")) {
                            fields[k] = fields[k].substring(0, fields[k].indexOf("."));
                        }
                        if (fields[k].contains("[")) {
                            fields[k] = fields[k].substring(0, fields[k].indexOf("["));
                        }
                        k++;
                    }
                    BorderlandsObject obj = BorderlandsObject.parseObject(d.dump);
                    for (k = 0; k < fields.length; k++) {
                        String com = null;
                        boolean done = false;
                        try {
                            ModelElement el;
                            String field = cfields[k];
                            Object val = obj.getField(cfields[k]);
                            if (val == null) {
                                Object v2 = obj.getField(fields[k]);
                                if (v2 == null) {
                                    val = "";
                                } else {
                                    throw new NullPointerException();
                                }
                            }
                            if (coms[k].getParent() instanceof HotfixWrapper) {
                                HotfixWrapper oldw = (HotfixWrapper) coms[k].getParent();
                                com = "set " + d.object + " " + field + " " + val;
                                el = new HotfixWrapper(oldw.getName(), oldw.getType(), oldw.getParameter(), Arrays.asList(new String[]{com}));
                            } else {
                                el = new SetCommand(d.object, field, val.toString());
                            }
                            patch.insertElementInto(el, inverted);
                            done = true;
                        } catch (NullPointerException e) {
                            //field does not exist
                            System.err.println("Field " + cfields[k] + " does not exist in " + d.object);
                        } catch (IndexOutOfBoundsException e) {
                            System.err.println("Field " + cfields[k] + " does not exist with those indices in " + d.object);
                        } catch (IllegalArgumentException e) {
                            System.err.println("The command '" + com + "' is not a valid command.");
                        }
                        if (!done) {
                            ModelElement el;
                            if (coms[k].getParent() instanceof HotfixWrapper) {
                                HotfixWrapper oldw = (HotfixWrapper) coms[k].getParent();
                                el = new HotfixWrapper(oldw.getName(), oldw.getType(), oldw.getParameter(), Arrays.asList(new String[]{coms[k].getCode()}));
                            } else {
                                el = new SetCommand(coms[k].getCode());
                            }
                            patch.insertElementInto(el, uninverted);
                        }

                    }
                });
            }

        }
        System.out.println("Took " + ((System.currentTimeMillis() - l) / 1000) + " seconds to invert " + commands + " commands");
    }

    private void analyzeCommand(SetCommand s, HashMap<String, HashSet<String>> classToObjectsMap, HashMap<String, HashSet<SetCommand>> objectToCommandsMap) {
        String obj = s.getObject();
        String clazz = DataManager.getDictionary().getObjectClass(obj);
        classToObjectsMap.putIfAbsent(clazz, new HashSet<>());
        classToObjectsMap.get(clazz).add(obj);
        objectToCommandsMap.putIfAbsent(obj, new HashSet<>());
        objectToCommandsMap.get(obj).add(s);
    }

    private void extractData(Category toBeInverted, Collection<SetCommand> codes, Collection<HotfixWrapper> wrappers) {
        for (ModelElement el : toBeInverted.getElements()) {
            if (el instanceof Category) {
                extractData((Category) el, codes, wrappers);
            } else if (el instanceof SetCommand) {
                codes.add((SetCommand) el);
            } else if (el instanceof HotfixWrapper) {
                wrappers.add((HotfixWrapper) el);
            }
        }
    }

}
