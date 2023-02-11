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
package blcmm.gui.tree;

import blcmm.gui.theme.ThemeManager;
import blcmm.model.*;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.model.properties.PropertyChecker;
import blcmm.utilities.Options;
import java.awt.Color;
import java.util.HashMap;

/**
 *
 * @author LightChaosman
 */
public class ColorGiver {

    private static final HashMap<Class, ThemeManager.ColorType> COLORMAP = new HashMap<>();

    static {
        // First up: color highlights for various kinds of commands
        COLORMAP.put(GlobalListOfProperties.MUTChecker.class, ThemeManager.ColorType.TreeMUTChecker);
        COLORMAP.put(GlobalListOfProperties.CommentChecker.class, ThemeManager.ColorType.TreeCommentChecker);
        COLORMAP.put(GlobalListOfProperties.CommentChecker.Say.class, ThemeManager.ColorType.TreeSpecialCommentChecker);
        COLORMAP.put(GlobalListOfProperties.CommentChecker.Exec.class, ThemeManager.ColorType.TreeSpecialCommentChecker);
        COLORMAP.put(GlobalListOfProperties.HotfixChecker.class, ThemeManager.ColorType.TreeHotfixChecker);
        COLORMAP.put(GlobalListOfProperties.CompleteClassChecker.class, ThemeManager.ColorType.TreeCompleteClassCommandChecker);

        // Now, outright syntax errors
        COLORMAP.put(GlobalListOfProperties.OpenedAndSavedInFilterToolChecker.class, ThemeManager.ColorType.TreeSyntaxError);
        COLORMAP.put(GlobalListOfProperties.HotfixSyntaxInNormalCommandChecker.class, ThemeManager.ColorType.TreeSyntaxError);
        COLORMAP.put(GlobalListOfProperties.IncompleteSetCommandChecker.class, ThemeManager.ColorType.TreeSyntaxError);
        COLORMAP.put(GlobalListOfProperties.InvalidArgumentForSquareBracketChecker.class, ThemeManager.ColorType.TreeSyntaxError);
        COLORMAP.put(GlobalListOfProperties.MismatchingBracketsChecker.class, ThemeManager.ColorType.TreeSyntaxError);
        COLORMAP.put(GlobalListOfProperties.HotfixImportErrorChecker.class, ThemeManager.ColorType.TreeSyntaxError);
        COLORMAP.put(GlobalListOfProperties.FieldSyntaxChecker.class, ThemeManager.ColorType.TreeSyntaxError);
        COLORMAP.put(GlobalListOfProperties.ObjectSyntaxChecker.class, ThemeManager.ColorType.TreeSyntaxError);

        // Content errors within the statements themselves
        COLORMAP.put(GlobalListOfProperties.NumberFieldChecker.class, ThemeManager.ColorType.TreeContentError);
        COLORMAP.put(GlobalListOfProperties.IntegerFieldChecker.class, ThemeManager.ColorType.TreeContentError);
        COLORMAP.put(GlobalListOfProperties.BooleanFieldChecker.class, ThemeManager.ColorType.TreeContentError);
        COLORMAP.put(GlobalListOfProperties.RestrictedFieldChecker.class, ThemeManager.ColorType.TreeContentError);
        COLORMAP.put(GlobalListOfProperties.EmptyCategoryChecker.class, ThemeManager.ColorType.TreeContentError);
        COLORMAP.put(GlobalListOfProperties.ClassHotfixChecker.class, ThemeManager.ColorType.TreeContentError);

        // Style warnings.  Not errors, or necessarily even wrong, but should
        // know what you're doing.
        COLORMAP.put(GlobalListOfProperties.IncompleteBVCChecker.class, ThemeManager.ColorType.TreeStyleWarning);
        COLORMAP.put(GlobalListOfProperties.MisMatchingQuotesChecker.class, ThemeManager.ColorType.TreeStyleWarning);
        COLORMAP.put(GlobalListOfProperties.GameWillOverwriteValueChecker.class, ThemeManager.ColorType.TreeStyleWarning);

    }

    public static final Color getColor(ModelElement element) {
        ThemeManager.ColorType dupType = (ThemeManager.ColorType) OverwriteChecker.getColor(element);
        Color c;
        for (PropertyChecker property : element.getTransientData().getProperties()) {
            if (property instanceof GlobalListOfProperties.IncompleteBVCChecker
                    && !Options.INSTANCE.getHighlightBVCErrors()) {
                continue;
            }
            ThemeManager.ColorType type = COLORMAP.get(property.getClass());
            if (!Options.INSTANCE.isInDeveloperMode()
                    && (type == ThemeManager.ColorType.TreeSyntaxError
                    || type == ThemeManager.ColorType.TreeStyleWarning
                    || type == ThemeManager.ColorType.TreeContentError)) {
                continue;
            }
            c = ThemeManager.getColor(type);
            if (dupType != null
                    && (Options.INSTANCE.isInDeveloperMode() || property.getPropertyDescriptionType() == PropertyChecker.DescType.Informational)
                    && GlobalListOfProperties.PROPERTY_COMPARATOR2.compare(property.getClass(), GlobalListOfProperties.MUTChecker.class) > 0) {
                return ThemeManager.getColor(dupType);
            }
            if (c != null) {
                return c;
            }
        }

        // Lastly, apply duplication coloring if we have any.
        if (dupType == null) {

            if (element.getParent() == null) {
                // If we're the root element, explicitly set text color so we
                // get bolded.
                return ThemeManager.getColor(ThemeManager.ColorType.TreeRootNode);
            } else if (element instanceof Category
                    && ((Category) element).getName().equals("mods")
                    && element.getParent() != null
                    && element.getParent().getParent() == null) {
                // If we're the top-level "mods" folder, also explicitly set
                // text color so we get bolded.
                return ThemeManager.getColor(ThemeManager.ColorType.TreeRootNode);
            } else {
                // Otherwise, don't set any explicit coloring.
                return null;
            }

        } else {
            return ThemeManager.getColor(dupType);
        }
    }

    public static final void reset(Category root) {
        OverwriteChecker.reset(root);
    }
}
