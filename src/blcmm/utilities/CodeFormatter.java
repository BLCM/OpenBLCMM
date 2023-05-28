/*
 * Copyright (C) 2018-2020  LightChaosman
 * Copyright (C) 2023 Christopher J. Kucera
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
package blcmm.utilities;

import blcmm.model.Comment;
import blcmm.model.ModelElement;
import blcmm.model.SetCMPCommand;
import blcmm.model.SetCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * A class to deal with code formatting and de-formatting, and generally
 * taking user-input mod code and turning it into formats more suitable for
 * the rest of the app to work with.
 *
 * This used to be a static inner class of blcmm.utilities.Utilities but IMO
 * it makes more sense out here as its own standalone class.  I've also pulled
 * in some highly-related methods from blcmm.gui.panels.EditPanel, since
 * they're so tightly related.
 *
 * @author LightChaosman
 */
public class CodeFormatter {

    private final static String INDENTATION = "    ";
    private final static HashSet<String> COMMANDS = new HashSet<> ();

    static {
        // TODO: https://github.com/BLCM/OpenBLCMM/issues/9
        COMMANDS.add("set");
        COMMANDS.add("set_cmp");
        COMMANDS.add("exec");
        COMMANDS.add("say");
    }

    /**
     * Splits freeform user-generated mod code into discrete "parts", ideally
     * separating out statements from each other, keeping comments separate,
     * etc.  This is used in a couple of different places in the processing
     * path, when parsing user code.
     *
     * @param modCode User-generated freeform mod code.
     * @return A list of Strings, each one of which is theoretically a discrete
     *    part of the mod code -- either a single statement, or a single comment
     *    line.
     */
    private static List<String> splitIntoParts(String modCode) {
        List<List<String>> statements = new ArrayList<>();
        for (String line : modCode.split("\n")) {
            StringTokenizer st = new StringTokenizer(line);
            if (st.hasMoreTokens()) {
                // Asssuming here that commands are case-insensitive, though I'd be
                // surprised if there were any cases of non-lowercase commands.  Also
                // checking for the presence of `#` as a start character (which is
                // not *required* for comments, but it's used with a bit of regularity),
                // and a `set` prefix (not as a full token).  That last is mostly for
                // backwards compatibility purposes with how BLCMM's always behaved,
                // but also having `set_` prefixes in Command Extension commands isn't
                // uncommon, so it's probably legitimately useful in many circumstances.
                String token = st.nextToken().toLowerCase();
                if (CodeFormatter.COMMANDS.contains(token)
                        || token.startsWith("#")
                        || token.startsWith("set")) {
                    statements.add(new ArrayList<>());
                }
            }
            if (statements.isEmpty()) {
                statements.add(new ArrayList<>());
            }
            statements.get(statements.size()-1).add(line);
        }
        List<String> toReturn = new ArrayList<> ();
        String statement;
        for (List<String> lines : statements) {
            statement = String.join("\n", lines);
            if (statement.length() > 0 && statement.substring(statement.length()-1).equals("\n")) {
                toReturn.add(statement.substring(0, statement.length()-1));
            } else {
                toReturn.add(statement);
            }
        }
        return toReturn;
    }

    /**
     * Splits the edit panel textarea into discrete "parts," ideally separating
     * separate commands into their own strings, and deformats the output to
     * have each statement on a single line (with spurious whitespace stripped,
     * etc).  If the first token in the string doesn't start with `set`, the
     * discrete parts will be taken to just be the individual contents of each
     * line.
     *
     * This method is the main entrypoint into CodeFormatter from the EditPanel
     * method to commit user code changes.  (Some checks are performed first,
     * but when the app gets to the point of actually translating user code
     * text into ModelElements, this is the first thing that gets hit.)
     *
     * @param modCode The mod code to split into parts
     * @return A list of strings, each one theoretically containing a single
     *     command.  The commands shouldn't contain any newlines.
     */
    public static List<String> splitIntoDeformattedParts(String modCode) {

        // If the block doesn't start with `set`, just split on lines and be
        // done with it.
        if (!modCode.toLowerCase().trim().startsWith("set")) {
            return Arrays.asList(modCode.split("\n"));
        }

        // Format the code to normalize things, which should hopefully make
        // the later processing a little more reliable
        String formatted = CodeFormatter.formatCode(modCode);

        // Now split into discrete parts
        List<String> parts = CodeFormatter.splitIntoParts(formatted);

        // Looping through parts, perform deformatting if possible to get the
        // statement all on a single line.
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (part.startsWith("set")) {
                int split = part.indexOf("\n");
                if (split != -1) {
                    if (split < part.length()-1) {
                        String head = part.substring(0, split);
                        String tail = part.substring(split + 1);
                        part = head.trim() + " " + CodeFormatter.deFormatCode(tail).trim();
                    } else {
                        part = part.substring(0, split).trim();
                    }
                }
            }
            parts.set(i, part);
        }
        return parts;
    }

    /**
     * Takes user-entered mod code, which may include multi-line
     * statements and the like, and may contain multiple statements, and
     * format to a more normalized string -- whitespace will be normalized
     * (indented by 4 spaces where appropriate, spaces on either side of
     * attribute equals signs, one "inner" attribute per line).  There will be
     * empty lines inbetween statements.
     *
     * Note that this is *not* the first step when EditPanel starts to process
     * user data after "OK" is hit.  First some syntax checks are run (which
     * *can* end up calling out to formatCode), then the whole dialog text
     * is passed through to splitIntoParts(), and then each part is individually
     * processed.
     *
     * @param original The user-entered code from an edit dialog or the like
     * @return A "normalized" string containing potentially more than one
     *  command.
     */
    public static String formatCode(String original) {
        // So formatCode() used to concat everything onto a single line and
        // then process it bit by bit, looking for the magic tokens `set` and
        // `set_cmp` to know where to inject newlines (it also adds in some
        // newlines once it detects that a paren clause has been fully closed).
        // The drawback there is that a token `set` inside a statement value
        // ends up getting broken 'cause it assumed it was a new command.  So,
        // this wrapper now splits the code into parts first and then formats
        // each part.  So long as a multiline statement doesn't have one of
        // set/set_cmp/say/exec as its first token, it should Do The Right
        // Thing.
        List<String> statements = CodeFormatter.splitIntoParts(original);
        for (int i=0; i<statements.size(); i++){
            String result = CodeFormatter.formatCodeSingleStatement(statements.get(i).replaceAll("\n", " ").replaceAll("[ ]+", " "));
            if (result.length() >= 2 && result.substring(result.length()-2).equals("\n\n")) {
                statements.set(i, result.substring(0, result.length()-2));
            } else {
                statements.set(i, result);
            }
        }
        return String.join("\n\n", statements);
    }

    /**
     * Takes a single user-entered mod code, which may span multiple
     * lines, and format to a normalized string, in the style described by
     * CodeFormatter.formatCode().  This method does handle a string which
     * contains more than one command, and under some circumstances could
     * end up outputting a string containing more than one.  That should be
     * pretty rare, though, now that CodeFormatter.formatCode() is
     * pre-processing the input.
     *
     * @param original The user-entered code from an edit dialog or the like
     * @return A "normalized" string
     */
    public static String formatCodeSingleStatement(String original) {

        StringBuilder sb = new StringBuilder();
        // I'm pretty sure that this was a total NOOP after we refactored these functions while
        // fixing https://github.com/BLCM/OpenBLCMM/issues/4
        //original = original.replaceAll("\n", "   ");
        int depth = 0;
        original = original.trim();
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);

            switch (c) {
                case '(': {
                    boolean b = true;
                    boolean d = false;
                    int l = 1;
                    while (i + l < original.length()) {
                        char c2 = original.charAt(i + l);
                        if (Character.isDigit(c2) || c2 == ' ' || c2 == ',') {
                            l++;
                        } else if (c2 == ')') {
                            d = true;
                            break;
                        } else {
                            b = false;
                            break;
                        }
                    }
                    if (b && d) {
                        sb.append(original.substring(i, i + l + 1));
                        i = i + l;
                    } else {
                        boolean lastIsPlus = sb.length() > 0 && sb.charAt(sb.length() - 1) == '+';
                        if (lastIsPlus) {
                            sb.setLength(sb.length() - 1);
                        }
                        removeTrailingWhiteSpace(sb);
                        sb.append("\n");
                        addIndentation(sb, depth);
                        if (lastIsPlus) {
                            sb.append("+");
                        }
                        sb.append(c);
                        depth++;
                        int j = 1;
                        while (i + j < original.length() && original.charAt(i + j) == ' ') {
                            j++;
                        }
                        if (i + j < original.length() && original.charAt(i + j) != '(') {
                            sb.append("\n");
                            addIndentation(sb, depth);
                        }
                    }
                    break;
                }
                case ')':
                    removeTrailingWhiteSpace(sb);
                    sb.append("\n");
                    depth--;
                    addIndentation(sb, depth);
                    sb.append(c);
                    if (depth == 0) {
                        sb.append("\n\n");
                    }
                    break;
                case '=':
                    boolean skipSpacingForEquals = shouldSkipSpacingOnEqualSignInsert(sb);
                    if (!skipSpacingForEquals && (i == 0 || original.charAt(i - 1) != ' ')) {
                        sb.append(" ");
                    }
                    sb.append(c);
                    if (!skipSpacingForEquals && (i + 1 >= original.length() || original.charAt(i + 1) != ' ')) {
                        sb.append(" ");
                    }
                    break;
                case ',': {
                    sb.append(c);
                    int j = 1;
                    while (i + j < original.length() && original.charAt(i + j) == ' ') {
                        j++;
                    }
                    if (depth == 0) {
                        //do nothing
                    } else if (i + j < original.length() && original.charAt(i + j) != '(') {
                        sb.append("\n");
                        addIndentation(sb, depth);
                    }
                    break;
                }
                case ' ': {
                    int j = 1;
                    while (sb.charAt(sb.length() - j) == ' ') {
                        j++;
                    }
                    if (sb.charAt(sb.length() - j) != '\n') {
                        sb.append(c);
                    }
                    break;
                }
                case '"': {
                    sb.append(c);
                    while (i < original.length() - 1 && (c = original.charAt(++i)) != '"') {
                        sb.append(c);
                    }
                    if (i < original.length() - 1 || (i == original.length() - 1 && original.charAt(i) == '"')) {
                        sb.append(c);
                    }
                    break;
                }
                default:
                    sb.append(c);
                    break;
            }

        }
        //putSetCommandsOnNewlines(sb);
        return sb.toString();
    }

    private static void removeTrailingWhiteSpace(StringBuilder sb) {
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }
    }

    private static void addIndentation(StringBuilder sb, int depth) {
        for (int j = 0; j < depth; j++) {
            sb.append(INDENTATION);
        }
    }

    private static boolean shouldSkipSpacingOnEqualSignInsert(StringBuilder sb) {//concise naming ftw
        int idx = sb.length() - 1;
        while (idx > 0 && sb.charAt(idx) != '\n') {
            idx--;
        }
        if (StringUtilities.substringStartsWith(sb, idx, "set")) {
            if (sb.charAt(idx + 3) == ' ') {
                return true;
            } else if (StringUtilities.substringStartsWith(sb, idx + 3, "_cmp") && sb.charAt(idx + 7) == ' ') {
                return true;
            }
        }
        return false;
    }

    /*
     * This is the code which used to end up breaking code which had a `set` token inside
     * its value, something which was a problem for Red Text Explainer, for instance.  (Also
     * technically `set_cmp`, though that was far less likely to show up.)
    @SuppressWarnings("empty-statement")
    private static void putSetCommandsOnNewlines(StringBuilder sb) {
        int idx = 0;
        while (idx < sb.length()) {
            if (sb.charAt(idx) == '"') {
                while (++idx < sb.length() && sb.charAt(idx) != '"');
                //idx is now at the end of the string, or at the index of the closing quote
            } else if (StringUtilities.substringStartsWith(sb, idx, "set")) {
                boolean prevIsWhitespace = idx == 0 || Character.isWhitespace(sb.charAt(idx - 1));
                boolean cmp = StringUtilities.substringStartsWith(sb, idx + 3, "_cmp");
                int nextIdx = idx + (cmp ? 7 : 3);
                boolean nextIsWhiteSpace = nextIdx < sb.length() && Character.isWhitespace(sb.charAt(nextIdx));
                if (prevIsWhitespace && nextIsWhiteSpace && idx > 0) {
                    sb.replace(idx - 1, idx, "\n\n");
                    idx = nextIdx + 1;
                }
            }
            idx++;
        }
    }
    /**/

    public static String deFormatCode(String original) {
        return removeNonQuotedSpaces(original.replaceAll("\n", " "));
    }

    public static String removeNonQuotedSpaces(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append(s.charAt(i++));
                while (i < s.length() && s.charAt(i) != '"') {
                    sb.append(s.charAt(i++));
                }
                if (i < s.length()) {
                    sb.append('"');
                }
            } else if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String deFormatCodeInnerNBrackets(String original, final int n) {
        List<Integer> stack = new ArrayList<>();
        Map<Integer, Integer> depthMap = new TreeMap<>();
        Map<Integer, Integer> closingMap = new TreeMap<>();
        int depth = 0;
        int maxdepth = 0;
        original = formatCode(original);
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (c == '(') {
                depth++;
                maxdepth = java.lang.Math.max(maxdepth, depth);
                for (int j = 0; j < stack.size(); j++) {
                    Integer start = stack.get(j);
                    int curDeepest = depthMap.get(start);
                    depthMap.put(start, java.lang.Math.max(curDeepest, depth - j));
                }
                stack.add(i);
                depthMap.put(i, 1);
            } else if (c == ')' && depth != 0) {//
                depth--;
                int start = stack.remove(stack.size() - 1);
                closingMap.put(start, i);
            }
        }
        TreeMap<Integer, String> replacements = new TreeMap<>();
        for (Integer start : depthMap.keySet()) {
            int d = depthMap.get(start);
            if (d <= n || (d == maxdepth && maxdepth < n)) {
                int endIdx = closingMap.getOrDefault(start, original.length());
                String val = deFormatCode(original.substring(start, endIdx));
                replacements.put(start, val);
            }
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < original.length()) {
            if (replacements.containsKey(i)) {
                int k = sb.length() - 1;
                while (k >= 0 && Character.isWhitespace(sb.charAt(k))) {
                    k--;
                }
                if (k == '=') {
                    while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                }
                sb.append(replacements.get(i).trim());
                i = closingMap.getOrDefault(i, original.length());
            } else {
                sb.append(original.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    public static String deFormatCodeForUserDialog(String code) {
        return deFormatCodeForUserDialog(code, 1);
    }

    public static String deFormatCodeForUserDialog(String code, int n) {
        return deFormatCodeForUserDialog(code, n, 15, 120);
    }

    public static String deFormatCodeForUserDialog(String code, int n,
            int maxLines, int maxLineLength) {

        String formattedCommand = CodeFormatter.deFormatCodeInnerNBrackets(code, n);
        StringBuilder sb = new StringBuilder();
        sb.append("<pre>");
        int linecount = 0;
        for (String line : formattedCommand.split("\n")) {
            linecount++;
            if (linecount > maxLines) {
                sb.append("...");
                break;
            }
            if (line.length() > maxLineLength) {
                sb.append(line.substring(0, maxLineLength - 3));
                sb.append("...");
            } else {
                sb.append(line);
            }
            sb.append("<br/>");
        }
        sb.append("</pre>");
        return sb.toString();
    }

    /**
     * Converts the specified (probably user-entered) `modCode` into a list
     * of ModelElements suitable for inclusion in a CompletePatch structure.
     * This is primarily just used by blcmm.gui.panels.EditPanel as part of
     * its processing of the main text area.
     *
     * @param modCode The user mod code, which may contain multiple statements.
     * @return A list of ModelElements derived from the mod code.
     */
    public static List<ModelElement> convertModCodeToModels(String modCode) {
        return CodeFormatter.convertPartsToModels(CodeFormatter.splitIntoParts(modCode));
    }

    /**
     * Converts the specified list of mod commands (one per String) into a
     * list of ModelElements suitable for inclusion in a CompletePatch
     * structure.
     *
     * @param parts A list of mod commands
     * @return A list of ModelElements drived from the string commands
     */
    public static List<ModelElement> convertPartsToModels(List<String> parts) {
        List<ModelElement> list = new ArrayList<>();

        for (String s : parts) {
            final ModelElement com;
            if (SetCommand.isValidCommand(s)) {
                if (s.startsWith("set_cmp")) {
                    com = new SetCMPCommand(s);
                } else {
                    com = new SetCommand(s);
                }
            } else if (s.startsWith("set_cmp") && s.length() > 7 && Character.isWhitespace(s.charAt(7))) {
                String[] split = s.split("\\s+");
                if (split.length < 3) {
                    com = new Comment(s);
                } else {
                    com = new SetCMPCommand(split[1], split[2], split.length > 3 ? split[3] : "", "");
                }
            } else if (s.startsWith("set") && s.length() > 3 && Character.isWhitespace(s.charAt(3))) {
                String[] split = s.split("\\s+");
                if (split.length < 3) {
                    com = new Comment(s);
                } else {
                    com = new SetCommand(split[1], split[2], "");
                }
            } else {
                com = new Comment(s);
            }
            list.add(com);
        }
        return list;
    }

}
