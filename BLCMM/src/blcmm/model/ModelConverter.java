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

import blcmm.plugins.pseudo_model.PCategory;
import blcmm.plugins.pseudo_model.PCommand;
import blcmm.plugins.pseudo_model.PComment;
import blcmm.plugins.pseudo_model.PHotfix;
import blcmm.plugins.pseudo_model.PModelElement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author LightChaosman
 */
public class ModelConverter {

    private ModelConverter() {
    }

    public static Category convertFromPseudo(PCategory cat, CompletePatch p) {
        Category root = new Category(cat.getName());
        root.setMutuallyExclusive(cat.isMutuallyExclusive());
        for (PModelElement el : cat.getChildren()) {
            if (el instanceof PCategory) {
                Category kid = convertFromPseudo((PCategory) el, p);
                kid.setParent(root);
                root.addElement(kid);
            } else if (el instanceof PComment) {
                for (String s : ((PComment) el).getComment().split("\n")) {
                    Comment c = new Comment(s);
                    c.setParent(root);
                    root.addElement(c);
                }
            } else if (el instanceof PHotfix) {
                PHotfix el2 = (PHotfix) el;
                HotfixWrapper wr = new HotfixWrapper(el2.getName(), el2.getType(), el2.getParameter());
                HotfixCommand com = el2.getCommand().startsWith("set_cmp")
                        ? new SetCMPCommand(el2.getCommand())
                        : new HotfixCommand(el2.getCommand());
                com.setParent(wr);
                p.setSelected(com, el2.isSelected());
                wr.addElement(com);
                wr.setParent(root);
                root.addElement(wr);
            } else if (el instanceof PCommand) {
                SetCommand s = new SetCommand(((PCommand) el).getCommand());
                s.setParent(root);
                p.setSelected(s, ((PCommand) el).isSelected());
                root.addElement(s);
            }
        }
        root.combineAdjecantHotfixWrappers();

        //We create a new Patch object so we can invoke the fixInvalidMUT on it.
        //This way we don't have to scan our entire current patch, since we already know that respects MUT
        CompletePatch tempPatch = new CompletePatch();
        tempPatch.setCurrentProfile(p.getCurrentProfile());
        tempPatch.setRoot(root);
        tempPatch.fixInvalidMUT();

        return root;
    }

    public static PCategory convertToPseudo(CompletePatch patch, boolean includeGBXFixes, boolean mergeMaps) {
        Category gbx = includeGBXFixes ? PatchIO.getGBXFixes(patch.getType()) : new Category("");
        Set<String> excludes = gbx.listHotfixMeta().stream().flatMap(w -> w.getElements().stream()).map(c -> c.getCode()).collect(Collectors.toSet());
        gbx.listHotfixMeta().forEach(w -> w.getElements().stream().filter(c -> c.getValue().startsWith("+(")).collect(Collectors.toSet()).forEach(w::removeElement));
        gbx.listHotfixMeta().stream().filter(w -> w.size() == 0).collect(Collectors.toSet()).forEach(gbx::removeElement);
        Category mergeCommands = new Category("Level merge commands");
        if (mergeMaps) {
            HashSet<ModelElement> excl2 = new HashSet<>();
            PatchIO.analyzeLevelMerges(patch.getType(), patch.getRoot(), excl2, mergeCommands);
            excl2.stream().filter(el -> el instanceof SetCommand).map(el -> ((SetCommand) el).getCode()).forEach(excludes::add);
        }
        PCategory gfixes = convertToPseudo(gbx, Collections.EMPTY_SET);
        PCategory merges = convertToPseudo(mergeCommands, Collections.EMPTY_SET);
        PCategory main = convertToPseudo(patch.getRoot(), excludes);
        PCategory result = new PCategory("save-ready-patch");
        result.addChild(gfixes);
        result.addChild(merges);
        result.addChild(main);

        return result;
    }

    private static PCategory convertToPseudo(Category cat, Set<String> commandsToExclude) {
        PCategory root = new PCategory(cat.getName());
        for (ModelElement el : cat.getElements()) {
            if (el instanceof Category) {
                root.addChild(convertToPseudo((Category) el, commandsToExclude));
            } else if (el instanceof Comment) {
                root.addChild(new PComment(((Comment) el).getComment()));
            } else if (el instanceof SetCommand) {
                if (((SetCommand) el).isSelected() && !commandsToExclude.contains(((SetCommand) el).getCode())) {
                    PCommand c = new PCommand(((SetCommand) el).getCode());
                    //c.setSelected(((SetCommand) el).isSelected());// We're only providing the selected part of the file anyway
                    root.addChild(c);
                }
            } else if (el instanceof HotfixWrapper) {
                for (HotfixCommand el2 : ((HotfixWrapper) el).getElements()) {
                    if (el2.isSelected() && !commandsToExclude.contains(((SetCommand) el2).getCode())) {
                        HotfixWrapper wrapper = (HotfixWrapper) el;
                        PHotfix h = new PHotfix(el2.getCode(), wrapper.getType(), wrapper.getParameter(), wrapper.getName());
                        //h.setSelected(((SetCommand) el).isSelected());// We're only providing the selected part of the file anyway
                        root.addChild(h);
                    }
                }
            }
        }
        return root;
    }

}
