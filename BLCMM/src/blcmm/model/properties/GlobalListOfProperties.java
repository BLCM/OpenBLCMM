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
package blcmm.model.properties;

import blcmm.data.lib.DataManager;
import blcmm.model.Category;
import blcmm.model.Comment;
import blcmm.model.HotfixCommand;
import blcmm.model.ModelElement;
import blcmm.model.PatchIO;
import blcmm.model.SetCMPCommand;
import blcmm.model.SetCommand;
import blcmm.utilities.Options;
import general.utilities.StringUtilities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 *
 * @author LightChaosman
 */
public class GlobalListOfProperties {

    public static final List<PropertyChecker> LIST;
    public static final List<PropertyChecker> DEPENDING_ON_CHILDREN;
    private final static HashMap<Class, Integer> PROPERTY_PRIORITY;

    public static final Comparator<PropertyChecker> PROPERTY_COMPARATOR = new Comparator<PropertyChecker>() {

        @Override
        public int compare(PropertyChecker t, PropertyChecker t1) {
            return PROPERTY_PRIORITY.get(t.getClass()) - PROPERTY_PRIORITY.get(t1.getClass());
        }
    };

    public static final Comparator<Class<? extends PropertyChecker>> PROPERTY_COMPARATOR2 = new Comparator<Class<? extends PropertyChecker>>() {
        @Override
        public int compare(Class<? extends PropertyChecker> t, Class<? extends PropertyChecker> t1) {
            return PROPERTY_PRIORITY.get(t) - PROPERTY_PRIORITY.get(t1);
        }
    };

    public static final Map<Class<? extends PropertyChecker>, List<PropertyChecker>> CLASS_TO_INSTANCE_MAP;

    static {
        List<PropertyChecker> checkers = new ArrayList<>();
        List<PropertyChecker> children = new ArrayList<>();

        // Constants we'll use on later checks
        String[] numberfields = new String[]{
            "BaseValueConstant",
            "BaseValueScaleConstant",
            "R", "G", "B", "A"};
        String[] integerfields = new String[]{
            "MinimumGrade",
            "MaximumGrade",};
        String[] booleanFields = new String[]{
            "bExternalSlot",
            "bRunEffectsAsSkill",
            "bDisabled", "IsEnabled",
            "bIncludeInFunStats",
            "bIncludeAlliesAsTarget",
            "bEnforceMinimumGrade",
            "bEnforceMaximumGrade",};
        HashMap<String, String[]> setValues = new HashMap<>();
        setValues.put("EffectTarget", new String[]{
            "TARGET_Allies",
            "TARGET_None",
            "TARGET_Pets",
            "TARGET_Self",
            "TARGET_Enemies",
            "TARGET_All",});
        setValues.put("ModifierType", new String[]{
            "MT_PreAdd",
            "MT_Scale",
            "MT_PostAdd",});

        // Ordering is important here because of the coloration which happens
        // when this is shown in the tree.  Therefore: we will first check for
        // things considered syntax errors.
        checkers.add(new OpenedAndSavedInFilterToolChecker());
        checkers.add(new HotfixImportErrorChecker());
        checkers.add(new HotfixSyntaxInNormalCommandChecker());
        checkers.add(new IncompleteSetCommandChecker());
        checkers.add(new InvalidArgumentForSquareBracketChecker());
        checkers.add(new MismatchingBracketsChecker());
        checkers.add(new FieldSyntaxChecker());
        checkers.add(new ObjectSyntaxChecker());

        // Now checks for "content" errors
        for (String field : numberfields) {
            checkers.add(new NumberFieldChecker(field));
        }
        for (String field : integerfields) {
            checkers.add(new IntegerFieldChecker(field));
        }
        for (String field : booleanFields) {
            checkers.add(new BooleanFieldChecker(field));
        }
        for (String field : setValues.keySet()) {
            checkers.add(new RestrictedFieldChecker(field, setValues.get(field)));
        }
        checkers.add(new EmptyCategoryChecker());
        checkers.add(new ClassHotfixChecker());
        checkers.add(new GameWillOverwriteValueChecker());

        //Next, the warning checkers
        checkers.add(new IncompleteBVCChecker());
        checkers.add(new MisMatchingQuotesChecker());

        // Next the informational ones which are just visual cues for users.
        checkers.add(new MUTChecker());
        checkers.add(new CompleteClassChecker());
        checkers.add(new HotfixChecker());
        checkers.add(new CommentChecker.Say());
        checkers.add(new CommentChecker.Exec());
        checkers.add(new CommentChecker());

        //And lastly, the properties that are completly invisible to users, that are used for other purposes
        checkers.add(new LeafTypeHotfixChecker());
        checkers.add(new LeafTypeCommandChecker());
        checkers.add(new LeafTypeCommentChecker());
        checkers.add(new LeafSelectedChecker());

        for (PropertyChecker property : checkers) {
            if (property.isDependantOnChildren()) {
                children.add(property);
            }
        }

        LIST = Collections.unmodifiableList(checkers);
        DEPENDING_ON_CHILDREN = Collections.unmodifiableList(children);

        Map<Class<? extends PropertyChecker>, List<PropertyChecker>> CLASS_TO_INSTANCE_MAP2 = new HashMap<>();
        Map<Class<? extends PropertyChecker>, List<PropertyChecker>> CLASS_TO_INSTANCE_MAP3 = new HashMap<>();

        PROPERTY_PRIORITY = new HashMap<>();
        for (int i = 0, k = 0; i < checkers.size(); i++) {
            Class<? extends PropertyChecker> c = checkers.get(i).getClass();
            if (!PROPERTY_PRIORITY.containsKey(c)) {
                PROPERTY_PRIORITY.put(c, k++);
            }
            CLASS_TO_INSTANCE_MAP2.putIfAbsent(c, new ArrayList<>());
            CLASS_TO_INSTANCE_MAP2.get(c).add(checkers.get(i));
        }
        for (Class c : CLASS_TO_INSTANCE_MAP2.keySet()) {
            CLASS_TO_INSTANCE_MAP3.put(c, Collections.unmodifiableList(CLASS_TO_INSTANCE_MAP2.get(c)));
        }
        CLASS_TO_INSTANCE_MAP = Collections.unmodifiableMap(CLASS_TO_INSTANCE_MAP3);

    }

    private static abstract class SyntaxPropertyChecker extends PropertyChecker {

        SyntaxPropertyChecker(boolean propagatingToAncestors, boolean dependantOnChildren) {
            super(propagatingToAncestors, dependantOnChildren);
        }

        @Override
        public final DescType getPropertyDescriptionType() {
            return DescType.SyntaxError;
        }
    }

    public static class MismatchingBracketsChecker extends SyntaxPropertyChecker {

        public MismatchingBracketsChecker() {
            super(true, false);
        }

        @Override
        @SuppressWarnings("empty-statement")
        public boolean checkProperty(ModelElement element) {
            if (element instanceof SetCommand) {
                String command = ((SetCommand) element).getCode();
                int reducedLength = command.length() - 1;
                int max0 = element instanceof SetCMPCommand ? 2 : 1;
                int zeroes = 0;
                int depth = 0;
                for (int i = 0; i < command.length(); i++) {
                    char c = command.charAt(i);
                    if (c == '"') {
                        while (i < reducedLength && (c = command.charAt(++i)) != '"');
                    }
                    if (c == '(') {
                        depth++;
                    } else if (c == ')') {
                        depth--;
                        if (depth < 0) {
                            return true;
                        } else if (depth == 0) {
                            zeroes++;
                            if (zeroes > max0) {
                                return true;
                            }
                        }
                    }
                }
                return depth != 0;
            }
            return false;
        }

        @Override
        public String getPropertyDescription() {
            return "This command has a faulty set of brackets";
        }
    }

    public static class HotfixSyntaxInNormalCommandChecker extends SyntaxPropertyChecker {

        public HotfixSyntaxInNormalCommandChecker() {
            super(true, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            if (!(element instanceof SetCommand) || element instanceof HotfixCommand) {
                return false;
            }
            SetCommand command = (SetCommand) element;
            return command.getField().contains("[") || command.getField().contains("]") || command.getField().contains(".") || command.getValue().startsWith("+(");
        }

        @Override
        public String getPropertyDescription() {
            return "This normal set command uses hotfix syntax";
        }
    }

    public static class InvalidArgumentForSquareBracketChecker extends SyntaxPropertyChecker {

        public InvalidArgumentForSquareBracketChecker() {
            super(true, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            if (element instanceof SetCommand) {
                SetCommand com = (SetCommand) element;//check if parent is hotfixwrapper?
                String comm = com.getField();

                int idx = comm.indexOf("[");
                while (idx != -1) {
                    int idx2 = comm.indexOf("]", idx);
                    if (idx2 == -1) {
                        return false;
                    }
                    String inner = comm.substring(idx + 1, idx2);
                    try {
                        Integer.parseInt(inner);
                    } catch (NumberFormatException e) {
                        return true;
                    }
                    idx = comm.indexOf("[", idx2);
                }
                return false;
            }
            return false;
        }

        @Override
        public String getPropertyDescription() {
            return "The argument in square braces must be an integer";
        }
    }

    public static class IncompleteSetCommandChecker extends SyntaxPropertyChecker {

        public IncompleteSetCommandChecker() {
            super(true, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            if (element instanceof Comment) {
                String comment = ((Comment) element).getComment().toLowerCase();
                if (false
                        || (comment.startsWith("set") && comment.length() > 3 && Character.isWhitespace(comment.charAt(3)))
                        || (comment.startsWith("set_cmp") && comment.length() > 7 && Character.isWhitespace(comment.charAt(7)))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getPropertyDescription() {
            return "This command has too few arguments";
        }
    }

    public static class ObjectSyntaxChecker extends SyntaxPropertyChecker {

        private final static String REGEX;

        static {
            // 2K Aus added some packages which start with `11B_` to TPS, which
            // will otherwise run afoul of this check.  Special-case that.
            // This only makes sense in TPS, but we'll not bother to check for
            // game type.
            //They also have subpackages that end with a question mark, we will
            //thus allow the last char to be a question mark
            String word1 = "(11B_)?[a-zA-Z][a-zA-Z0-9_-]*";
            String word2 = /*  */ "[a-zA-Z0-9_][a-zA-Z0-9_-]*(\\?)?";
            String REGEX1 = "(" + word1 + ")((\\.|:)" + word2 + ")*";
            String classWrapper = "[a-zA-Z0-9_]*";
            REGEX = "(" + REGEX1 + ")|((" + classWrapper + ")'(" + REGEX1 + ")')";
        }

        public ObjectSyntaxChecker() {
            super(true, false);
        }

        @Override
        public boolean checkProperty(ModelElement el) {
            if (!(el instanceof SetCommand)) {
                return false;
            }
            return !((SetCommand) el).getObject().matches(REGEX) || ((SetCommand) el).getObject().chars().filter(i -> i == ':').count() > 1;
        }

        @Override
        public String getPropertyDescription() {
            return "Object syntax error";
        }
    }

    public static class FieldSyntaxChecker extends SyntaxPropertyChecker {

        private static final String REGEX;

        static {
            String word = "[a-zA-Z_][a-zA-Z0-9_]*(\\[([0-9]|[1-9][0-9]*)\\])?";
            REGEX = "((" + word + ")\\.)*(" + word + ")";
        }

        public FieldSyntaxChecker() {
            super(true, false);
        }

        @Override
        public boolean checkProperty(ModelElement el) {
            if (!(el instanceof SetCommand)) {
                return false;
            }
            return !((SetCommand) el).getField().matches(REGEX);
        }

        @Override
        public String getPropertyDescription() {
            return "Field syntax error";
        }
    }

    public static class HotfixImportErrorChecker extends SyntaxPropertyChecker {

        public HotfixImportErrorChecker() {
            super(true, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            return (element instanceof Category
                    && ((Category) element).getName().equals(
                            PatchIO.INVALID_HOTFIX_STRING));
        }

        @Override
        public String getPropertyDescription() {
            return "Invalid hotfixes found in file - could not be fully imported";
        }

    }

    public static class OpenedAndSavedInFilterToolChecker extends PropertyChecker {//We don't use a syntax checker, so the color and tooltip also show up when developer mode is not enabled.

        public OpenedAndSavedInFilterToolChecker() {
            super(true, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            if (element instanceof Category) {
                Category cat = (Category) element;
                if (cat.getName().equals(PatchIO.FT_BLCMM_SAVED_STRING)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public DescType getPropertyDescriptionType() {
            return DescType.Informational;
        }

        @Override
        public String getPropertyDescription() {
            return PatchIO.FT_BLCMM_SAVED_STRING;
        }
    }

    private static abstract class ContentPropertyChecker extends PropertyChecker {

        ContentPropertyChecker(boolean propagatingToAncestors, boolean dependantOnChildren) {
            super(propagatingToAncestors, dependantOnChildren);
        }

        @Override
        public final DescType getPropertyDescriptionType() {
            return DescType.ContentError;
        }
    }

    private static abstract class AbstractFieldChecker extends ContentPropertyChecker {

        protected final String field;

        protected AbstractFieldChecker(String field) {
            super(true, false);
            this.field = field.toLowerCase();
        }

        @Override
        public final boolean checkProperty(ModelElement el) {
            if (el instanceof SetCommand) {
                return checkProperty(el, new Hints(null, ((SetCommand) el).getField().toLowerCase(), ((SetCommand) el).getValue().toLowerCase()));
            }
            return false;
        }

        @Override
        public final boolean checkProperty(ModelElement el, Hints hints) {
            if (el instanceof SetCommand) {
                String commandField = hints.lowerCaseField;
                String value = hints.lowerCaseValue;
                if (commandField.endsWith(field) && commandField.length() > field.length() && commandField.charAt(commandField.length() - field.length() - 1) == '.') {
                    if (lookAheadForInvalidValue(value, 0)) {
                        return true;
                    }
                }
                int idx = -1;
                outer:
                while ((idx = value.indexOf(field, idx + 1)) != -1) {
                    if (idx > 0) {
                        char c = value.charAt(idx - 1);
                        if (!(Character.isWhitespace(c) || c == ',' || c == '(')) {
                            continue;
                        }
                    }
                    int idx2 = idx + field.length();
                    boolean equals = false;
                    while (idx2 < value.length() && (Character.isWhitespace(value.charAt(idx2)) || value.charAt(idx2) == '=')) {
                        if (value.charAt(idx2) == '=') {
                            if (equals) {
                                return false;
                            }
                            equals = true;
                        }
                        idx2++;
                    }
                    if (idx2 == value.length() || idx2 == idx + field.length() || !equals) {
                        return false;
                    }
                    if (lookAheadForInvalidValue(value, idx2)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Retruns true if an invalid value is found, starting at fromIndex,
         * parsing for as long as is needed to determine invalidness
         *
         * @param code
         * @param fromIndex
         * @return
         */
        protected abstract boolean lookAheadForInvalidValue(String code, int fromIndex);
    }

    public static class RestrictedFieldChecker extends AbstractFieldChecker {

        private final String[] validValues;

        public RestrictedFieldChecker(String field, Collection<String> validValues) {
            this(field, validValues.toArray(new String[0]));
        }

        public RestrictedFieldChecker(String field, String... validValues) {
            super(field);
            this.validValues = validValues;
        }

        @Override
        protected boolean lookAheadForInvalidValue(String code, int fromIndex) {
            for (String valid : validValues) {
                if (fromIndex + valid.length() <= code.length() && code.substring(fromIndex, fromIndex + valid.length()).equalsIgnoreCase(valid)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String getPropertyDescription() {
            return String.format("The field %s must have one of the following values: %s", field, Arrays.toString(validValues));
        }

    }

    public static class IntegerFieldChecker extends AbstractFieldChecker {

        public IntegerFieldChecker(String field) {
            super(field);
        }

        @Override
        protected boolean lookAheadForInvalidValue(String code, int fromIndex) {
            int idx3 = fromIndex;
            char c;
            while (idx3 < code.length() && (c = code.charAt(idx3)) != ')' && c != ',' && !Character.isWhitespace(c)) {
                idx3++;
            }
            try {
                Integer.parseInt(code.substring(fromIndex, idx3));
            } catch (NumberFormatException e) {
                return true;
            }
            return false;
        }

        @Override
        public String getPropertyDescription() {
            return String.format("The field %s must be an integer", field);
        }

    }

    public static class NumberFieldChecker extends AbstractFieldChecker {

        public NumberFieldChecker(String field) {
            super(field);
        }

        @Override
        protected boolean lookAheadForInvalidValue(String code, int fromIndex) {
            int idx3 = fromIndex;
            char c;
            while (idx3 < code.length() && (c = code.charAt(idx3)) != ')' && c != ',' && !Character.isWhitespace(c)) {
                idx3++;
            }
            try {
                Double.parseDouble(code.substring(fromIndex, idx3));
            } catch (NumberFormatException e) {
                return true;
            }
            return false;
        }

        @Override
        public String getPropertyDescription() {
            return String.format("The field %s must be a number", field);
        }

    }

    public static class BooleanFieldChecker extends RestrictedFieldChecker {

        public BooleanFieldChecker(String field) {
            super(field, "True", "False");
        }
    }

    public static class ClassHotfixChecker extends ContentPropertyChecker {

        public ClassHotfixChecker() {
            super(true, false);
        }

        @Override
        public boolean checkProperty(ModelElement el) {
            if (!(el instanceof HotfixCommand)) {
                return false;
            }
            HotfixCommand s = (HotfixCommand) el;
            String object = s.getObject();
            Collection<String> allClasses = DataManager.getDictionary().getAllClasses();
            return allClasses.contains(object.toLowerCase());
        }

        @Override
        public String getPropertyDescription() {
            return "Hotfixes can not be applied to an entire class, only to specific objects.";
        }
    }

    public static class EmptyCategoryChecker extends ContentPropertyChecker {

        public EmptyCategoryChecker() {
            super(true, true);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            if (element instanceof Category) {
                return ((Category) element).size() == 0;
            }
            return false;
        }

        @Override
        public String getPropertyDescription() {
            return "This category is empty";
        }
    }

    private static abstract class WarningPropertyChecker extends PropertyChecker {

        WarningPropertyChecker(boolean propagatingToAncestors, boolean dependantOnChildren) {
            super(propagatingToAncestors, dependantOnChildren);
        }

        @Override
        public final DescType getPropertyDescriptionType() {
            return DescType.Warning;
        }

    }

    public static class IncompleteBVCChecker extends WarningPropertyChecker {

        private static final String A = "basevalueconstant";
        private static final String B = "basevalueattribute";
        private static final String C = "initializationdefinition";
        private static final String D = "basevaluescaleconstant";

        public IncompleteBVCChecker() {
            super(false, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            if (element instanceof SetCommand) {
                return checkProperty(element, new Hints(null, null, ((SetCommand) element).getValue().toLowerCase()));
            }
            return false;
        }

        @Override
        public boolean checkProperty(ModelElement element, Hints hints) {
            if (element instanceof SetCommand) {
                final String value = hints.lowerCaseValue;
                int curIdx = 0;
                while (curIdx < value.length()) {
                    if (value.charAt(curIdx) == '(') {
                        if (StringUtilities.substringStartsWith(value, curIdx + 1, A)
                                || StringUtilities.substringStartsWith(value, curIdx + 1, B)
                                || StringUtilities.substringStartsWith(value, curIdx + 1, C)
                                || StringUtilities.substringStartsWith(value, curIdx + 1, D)) {
                            int counter = 0;
                            int idx = curIdx + A.length();//a is the shortest option
                            while (++idx < value.length()) {
                                char x = value.charAt(idx);
                                if (x == ')') {
                                    if (counter != 3) {
                                        return true;
                                    } else {
                                        break;
                                    }
                                } else if (x == ',') {
                                    counter++;
                                }
                            }
                        }
                    }
                    curIdx++;
                }
            }
            return false;
        }

        @Override
        public String getPropertyDescription() {
            return "This command contains an incomplete BVC/BVA/ID/BVSC tuple.";
        }
    }

    public static class MisMatchingQuotesChecker extends WarningPropertyChecker {

        public MisMatchingQuotesChecker() {
            super(false, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            if (!(element instanceof SetCommand)) {
                return false;
            }
            String val = ((SetCommand) element).getValue();
            int idx = -1;
            int counter = 0;
            while ((idx = val.indexOf("\"", idx + 1)) != -1) {
                counter++;
            }
            return counter % 2 == 1;
        }

        @Override
        public String getPropertyDescription() {
            return "This command has mismatching quotes";
        }

    }

    public static class GameWillOverwriteValueChecker extends WarningPropertyChecker {

        private static final List<BiPredicate<String, String>> PREDICATES = new ArrayList<>();
        private static final Function<String, String> JUST_LAST_PART = (s) -> {
            int idx = s.lastIndexOf("."), idx2 = s.lastIndexOf("_");
            String res = idx == -1 ? s : s.substring(idx + 1, idx2 < idx ? s.length() : idx2);
            return res;
        };

        static {
            //Predicates are tested using lowercase versions of the object & field of the commands!!
            PREDICATES.add((object, field) -> field.equals("delay") /*         */ && JUST_LAST_PART.apply(object).equals("behavior_delay"));
            PREDICATES.add((object, field) -> field.startsWith("conditions") /**/ && JUST_LAST_PART.apply(object).equals("behavior_randombranch"));
            PREDICATES.add((object, field) -> field.startsWith("value") /*     */ && JUST_LAST_PART.apply(object).startsWith("behavior_compare"));
            PREDICATES.add((object, field) -> field.length() == 1 /*           */ && JUST_LAST_PART.andThen((s) -> s.startsWith("behavior_") && s.endsWith("math")).apply(object));

            //PREDICATES.add((object, field) -> field.equals("damageradiusformula") && JUST_LAST_PART.apply(object).equals("behavior_explode"));
        }

        public GameWillOverwriteValueChecker() {
            super(false, false);
        }

        @Override
        public final boolean checkProperty(ModelElement el) {
            if (el instanceof SetCommand) {
                return checkProperty(el, new Hints(((SetCommand) el).getObject().toLowerCase(), ((SetCommand) el).getField().toLowerCase(), null));
            }
            return false;
        }

        @Override
        public boolean checkProperty(ModelElement element, Hints hints) {
            if (element instanceof SetCommand) {
                for (BiPredicate pred : PREDICATES) {
                    if (pred.test(hints.lowerCaseObject, hints.lowerCaseField)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public String getPropertyDescription() {
            return "This command *may* be overwritten by the game, so it might have no effect - Check in-game";
        }

    }

    private static abstract class InformationalPropertyChecker extends PropertyChecker {

        InformationalPropertyChecker(boolean propagatingToAncestors, boolean dependantOnChildren) {
            super(propagatingToAncestors, dependantOnChildren);
        }

        @Override
        public final DescType getPropertyDescriptionType() {
            return DescType.Informational;
        }
    }

    public static class MUTChecker extends InformationalPropertyChecker {

        public MUTChecker() {
            super(Options.INSTANCE.getPropagateMUTNotification(), false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            return element instanceof Category && ((Category) element).isMutuallyExclusive();
        }

        @Override
        public String getPropertyDescription() {
            return "This folder contains mutually-exclusive options";
        }
    }

    public static class CompleteClassChecker extends InformationalPropertyChecker {

        public CompleteClassChecker() {
            super(false, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            if (!(element instanceof SetCommand) || element instanceof HotfixCommand) {
                return false;
            }
            SetCommand s = (SetCommand) element;
            String object = s.getObject();
            Collection<String> allClasses = DataManager.getDictionary().getAllClasses();
            return allClasses.contains(object.toLowerCase());
        }

        @Override
        public String getPropertyDescription() {
            return "This command affects ALL objects of a certain class";
        }
    }

    public static class CommentChecker extends InformationalPropertyChecker {

        public CommentChecker() {
            super(false, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            return element instanceof Comment;
        }

        @Override
        public String getPropertyDescription() {
            return null;
        }

        public static class Say extends CommentChecker {

            @Override
            public boolean checkProperty(ModelElement element) {
                return super.checkProperty(element)
                        && element.toString().startsWith("say")
                        && element.toString().length() > 3
                        && Character.isWhitespace(element.toString().charAt(3));
            }
        }

        public static class Exec extends CommentChecker {

            @Override
            public boolean checkProperty(ModelElement element) {
                return super.checkProperty(element)
                        && element.toString().startsWith("exec")
                        && element.toString().length() > 4
                        && Character.isWhitespace(element.toString().charAt(4));
            }
        }
    }

    public static class HotfixChecker extends InformationalPropertyChecker {

        public HotfixChecker() {
            super(false, false);
        }

        @Override
        public boolean checkProperty(ModelElement element) {
            return element instanceof HotfixCommand;
        }

        @Override
        public String getPropertyDescription() {
            return null;
        }
    }

    private static abstract class LeafTypeChecker extends PropertyChecker {

        protected LeafTypeChecker() {
            super(true, false);
        }

        @Override
        public DescType getPropertyDescriptionType() {
            return DescType.Invisible;
        }

        @Override
        public String getPropertyDescription() {
            return null;
        }
    }

    public static class LeafTypeCommentChecker extends LeafTypeChecker {

        public LeafTypeCommentChecker() {
        }

        @Override
        public boolean checkProperty(ModelElement el) {
            return el instanceof Comment;
        }

    }

    public static class LeafTypeCommandChecker extends LeafTypeChecker {

        public LeafTypeCommandChecker() {
        }

        @Override
        public boolean checkProperty(ModelElement el) {
            return el instanceof SetCommand;
        }

    }

    public static class LeafTypeHotfixChecker extends LeafTypeChecker {

        public LeafTypeHotfixChecker() {
        }

        @Override
        public boolean checkProperty(ModelElement el) {
            return el instanceof HotfixCommand;
        }
    }

    public static class LeafSelectedChecker extends LeafTypeChecker {

        public LeafSelectedChecker() {
        }

        @Override
        public boolean checkProperty(ModelElement el) {//TODO make this Enableable, not Set
            return el instanceof SetCommand && ((SetCommand) el).isSelected();
        }
    }

}
