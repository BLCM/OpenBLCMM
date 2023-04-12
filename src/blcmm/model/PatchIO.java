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
package blcmm.model;

import blcmm.Meta;
import blcmm.model.attrparser.LevelDepArray;
import blcmm.model.attrparser.LevelDepParser;
import blcmm.model.attrparser.LevelDepString;
import blcmm.model.attrparser.LevelDepStruct;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.utilities.GameDetection;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.ImportAnomalyLog;
import blcmm.utilities.OSInfo;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author LightChaosman
 */
public class PatchIO {

    //List of forbidden words to use in user-input fields, since they can mess up parsing.
    public static List<String> FORBIDDEN_KEYWORDS = Collections.unmodifiableList(Arrays.asList(new String[]{
        "<code>", "</code>", "<key>", "</key>", "<value>", "</value>", "<profile = ", "<on>", "<off>", "<hotfix>", "<MUT>", "<inProfile", "<comment>", "</comment>",}));
    public static List<String> FORBIDDEN_CATEGORY_NAMES = Collections.unmodifiableList(Arrays.asList(new String[]{
        "code", "key", "value", "value", "profile = ", "on", "off", "hotfix", "MUT", "inProfile", "comment"}));

    // String used to indicate invalid hotfixes which couldn't be imported properly
    public static final String INVALID_HOTFIX_STRING = "!!! Invalid hotfixes in the file that could not be converted, and that don't work in-game !!!";
    public static final String FT_UPDATE_STRING = "#<!!!You opened a file saved with BLCMM in FilterTool. Please update to BLCMM to properly open this file!!!>";
    private static final String FT_UPDATE_STRING_TO_CHECK = FT_UPDATE_STRING.substring(2, FT_UPDATE_STRING.length() - 1);
    public static final String FT_BLCMM_SAVED_STRING = "This was made in BLCMM, opened in Filtertool, then saved by FilterTool. This file is corrupt and unrepairable. Obtain a new copy!";

    public static enum SaveFormat {
        BLCMM, FT, STRUCTURELESS
    }

    //Interface to obtain new readers for objects, so we can abstract away from just files.
    //Since we need to "reset" readers to pre-read, and BufferedReaders don't provide adequate functionality for that, we use this workaround
    private static interface ReaderProvider<O> {

        public static final ReaderProvider<File> FILE_READER_PROVIDER = FileReader::new;
        public static final ReaderProvider<String> STRING__READER_PROVIDER = StringReader::new;

        public Reader createNewReader(O o) throws IOException;
    }

    private static final int SAVE_VERSION = 1;
    public static final String LINEBREAK = System.getProperty("line.separator");

    /**
     * Parses the provided string and returns the encoded Category and all it's
     * children.
     *
     * @param f the file to parse
     *
     * @throws IOException
     * @return
     */
    public static CompletePatch parse(File f) throws IOException {
        return parseInternal(f, ReaderProvider.FILE_READER_PROVIDER, f.getName());
    }

    public static CompletePatch parse(String s) throws IOException {
        return parseInternal(s, ReaderProvider.STRING__READER_PROVIDER, "Internal String");
    }

    private static <O> CompletePatch parseInternal(O o, ReaderProvider<O> p, String filename) throws IOException {
        final int preRead = 3;
        String[] line = new String[preRead];
        CompletePatch res = handleInvalidFiles(p, o, preRead, line, filename);
        if (res != null) {
            return res;
        }
        BufferedReader br = new BufferedReader(p.createNewReader(o));
        if (line[0].trim().startsWith("<BLCMM") || line[0].trim().startsWith("<category") || line[0].trim().startsWith("<code")) {
            res = new BLCMMParser().parse(br, filename);
        } else if (line[0].toLowerCase().startsWith("start") && filename.endsWith(".hotfix")) {
            res = new HotfixParser().parse(br, filename);
        } else {
            res = new FTParser().parse(br, filename);//FT parser will handle "anything"
        }
        br.close();
        res.fixInvalidMUT();
        return res;
    }

    private static <O> CompletePatch handleInvalidFiles(ReaderProvider<O> p, O o, final int preRead, String[] line, String filename) throws IllegalArgumentException, IOException {
        BufferedReader br = new BufferedReader(p.createNewReader(o));
        for (int i = 0; i < preRead; i++) {
            line[i] = removeGarbageCharacters(br.readLine());
            if (line[i] != null && line[i].trim().isEmpty()) {
                i--;
            }
        }
        br.close();

        //Check for empty files
        if (line[0] == null || line[0].trim().isEmpty()) {

            //empty file
            CompletePatch newPatch = new FTParser().parse(new BufferedReader(new StringReader("#<patch>\n#</patch>")), "");
            ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(
                    filename,
                    ImportAnomalyLog.ImportAnomalyType.EmptyFile,
                    "The file '" + filename + "' was empty.",
                    newPatch));
            return newPatch;
        }

        if (o instanceof File) {
            File f = (File) o;
            // This function returns the MIME Type of the given file (f in this case).
            // If you need a slightly inclusive list of MIME Types:
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Complete_list_of_MIME_types .

            String mimeType = Files.probeContentType(f.toPath());

            // Probably best to do this since we return null later anyways.
            // Note that if the file has no extension, `Files.probeContentType` will
            // return "text/html" for BLCMM-formatted files, so we're special-casing
            // it here.
            if (mimeType == null || mimeType.equals("text/plain") || line[0].startsWith("<BLCMM")) {
                return null;
            } // RAR File time!
            else if (mimeType.equals("application/x-rar-compressed")) {
                CompletePatch newPatch = new FTParser().parse(new BufferedReader(new StringReader("#<patch>\n#</patch>")), "");
                String extension = filename.split("\\.(?=[^\\.]+$)")[1];
                ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(filename,
                        ImportAnomalyLog.ImportAnomalyType.RARfile,
                        "The file '" + filename + "' is a " + extension + " extension file. Please extract the file using WinRAR / 7zip.",
                        newPatch));
                return newPatch;
            } // Now its time to check for HTML Files!
            else if (mimeType.equals("text/html")) {
                String relevantline = null;
                int idx = -1;
                if (line[2] != null && line[2].contains("href=\"https://github.com")) {
                    idx = line[2].indexOf("href=\"https://github.com") + "href=\"".length();
                    relevantline = line[2];
                } else {
                    try (BufferedReader br2 = new BufferedReader(p.createNewReader(o))) {
                        String l;
                        while ((l = br2.readLine()) != null) {
                            if (l.contains("href=\"https://github.com/BLCM/BLCMods/blob")) {
                                idx = l.indexOf("href=\"https://github.com/BLCM/BLCMods/blob") + "href=\"".length();
                                relevantline = l;
                                break;
                            }
                        }
                    }
                }
                if (idx != -1 && relevantline != null) {
                    int idx2 = relevantline.indexOf("\"", idx);

                    String url1 = relevantline.substring(idx, idx2);
                    String url2 = url1.replace(//
                            "https://github.com/BLCM/BLCMods/blob/master/",
                            "https://raw.githubusercontent.com/BLCM/BLCMods/master/");

                    String downloaded = Utilities.downloadFileToString(url2);
                    if (downloaded != null) {
                        CompletePatch patch = parse(downloaded);
                        ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(
                                filename,
                                ImportAnomalyLog.ImportAnomalyType.HTMLFile,
                                "The file '" + filename + "' is an HTML file. Downloading mod linked inside.",
                                patch));
                        return patch;
                    }
                }
                throw new IllegalArgumentException("The file you provided is an HTML file.\n"
                        + "It seems you download the page containing the mod, instead of the mod itself.\n"
                        + "To download a mod, click the 'Raw' button, and save the file you find there.");
            } // When we're opening some other MIME Type of file.
            else {
                CompletePatch newPatch = new FTParser().parse(new BufferedReader(new StringReader("#<patch>\n#</patch>")), "");
                String extension = filename.split("\\.(?=[^\\.]+$)")[1];
                ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(filename,
                        ImportAnomalyLog.ImportAnomalyType.IncorrectType,
                        "It looks like you opened a " + extension + " file. From what we can tell, this file is an unsupported file type.",
                        newPatch));

                return newPatch;
            }
        }

        return null;
    }

    private static String removeGarbageCharacters(String s) {
        return s == null ? null : s.replace(new String(new char[]{(char) 0}), "").replace(new String(new char[]{(char) 65533}), "");
    }

    public static String escape(String string) {
        return string.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String unescape(String substring) {
        return substring.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    /**
     * The superclass of all the parsers.
     */
    private static abstract class Parser {

        protected abstract CompletePatch parse(BufferedReader br, String filename) throws IOException;
    }

    /**
     * Parses an old-style ".hotfix" file.  It's a *bit* silly to keep this
     * maintained given that this format basically doesn't exist anymore, but
     * whatever.
     *
     * For some details on this ancient format, see: https://github.com/mystise/BL2_Converter
     */
    private final static class HotfixParser extends Parser {

        @Override
        protected CompletePatch parse(BufferedReader br, String filename) throws IOException {
            String name = filename.contains(".") ? filename.substring(0, filename.indexOf(".")) : filename;
            try {
                CompletePatch patch = new CompletePatch();
                patch.createNewProfile("default");
                Profile profile = patch.getCurrentProfile();
                patch.setPatchSource(CompletePatch.PatchSource.HOTFIX);
                Category root = new Category(name);
                patch.setRoot(root);
                ArrayList<HotfixWrapper> wrappers = parseOldStyleHotfixFile(br);
                for (HotfixWrapper wrapper : wrappers) {
                    wrapper.setParent(root);
                    root.addElement(wrapper);
                    for (HotfixCommand command : wrapper.getElements()) {
                        command.turnOnInProfile(profile);
                        command.profileChanged(profile);
                    }
                }
                return patch;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }


        /**
         * Converts an old-style `.hotfix`-formatted file to a series of HotfixWrapper objects.
         *
         * For some details on this ancient format, see: https://github.com/mystise/BL2_Converter
         *
         * @param br A BufferedReader from which to read the file data
         * @return The list of new HotfixCommand objects
         * @throws IOException
         */
        private static ArrayList<HotfixWrapper> parseOldStyleHotfixFile(BufferedReader br)
                throws IllegalArgumentException, IOException {
            ArrayList<HotfixWrapper> wrappers = new ArrayList<>();
            HotfixWrapper wrapper = null;
            HotfixCommand newCommand;
            String line;
            String[] tokens;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.equals("")) {
                    continue;
                }
                // I'm actually not sure if any commenting mechanisms were allowed, but whatever.
                if (line.charAt(0) == '#') {
                    continue;
                }

                // Split the string and see what we have.  We're being pretty precise about
                // how many tokens we expect, which the original code may not have done, and
                // also matching the control parameters case-insensitively, which probably
                // wasn't the original behavior.  Also possibly we're more fussy about lines
                // we don't understand?  Whatever.  It's not like this format functionally
                // exists anymore.
                tokens = line.split("\\s+", 2);
                switch (tokens[0].toLowerCase()) {

                    case "start":
                        tokens = line.split("\\s+", 3);
                        if (tokens.length < 2) {
                            throw new IllegalArgumentException("Invalid hotfix start line: " + line);
                        }
                        switch (tokens[1].toLowerCase()) {
                            case "patch":
                                if (tokens.length > 2) {
                                    throw new IllegalArgumentException("Invalid Patch hotfix start line: " + line);
                                }
                                wrapper = new HotfixWrapper("Hotfix", HotfixType.PATCH, "");
                                wrappers.add(wrapper);
                                break;

                            case "level":
                                if (tokens.length != 3) {
                                    throw new IllegalArgumentException("Invalid Level hotfix start line: " + line);
                                }
                                wrapper = new HotfixWrapper("Hotfix", HotfixType.LEVEL, tokens[2]);
                                wrappers.add(wrapper);
                                break;

                            case "ondemand":
                                if (tokens.length != 3) {
                                    throw new IllegalArgumentException("Invalid Level hotfix start line: " + line);
                                }
                                wrapper = new HotfixWrapper("Hotfix", HotfixType.ONDEMAND, tokens[2]);
                                wrappers.add(wrapper);
                                break;

                            default:
                                throw new IllegalArgumentException("Unknown hotfix start parameter: " + tokens[1]);
                        }
                        break;

                    case "set":
                        if (wrapper == null) {
                            throw new IllegalArgumentException("Got hotfix without hotfix start parameters: " + line);
                        }
                        tokens = line.split("\\s+", 4);
                        if (tokens.length < 4) {
                            throw new IllegalArgumentException("Invalid hotfix set: " + line);
                        }
                        newCommand = new HotfixCommand(tokens[1], tokens[2], tokens[3]);
                        newCommand.setParent(wrapper);
                        wrapper.addElement(newCommand);
                        break;

                    case "set_cmp":
                        if (wrapper == null) {
                            throw new IllegalArgumentException("Got hotfix without hotfix start parameters: " + line);
                        }
                        tokens = line.split("\\s+", 5);
                        if (tokens.length < 5) {
                            throw new IllegalArgumentException("Invalid hotfix set_cmp: " + line);
                        }
                        newCommand = new SetCMPCommand(tokens[1], tokens[2], tokens[3], tokens[4]);
                        newCommand.setParent(wrapper);
                        wrapper.addElement(newCommand);
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown line in hotfix file: " + line);
                }
            }

            return wrappers;
        }
    }

    private final static class FTParser extends Parser {

        private static String[] fixes = new String[2];
        private static SetCommand[] fixes2 = new SetCommand[2];
        private static boolean OLD_PARSE;

        private static void handleInvalidHotfix(CompletePatch patch, String s) {
            Category root = patch.getRoot();
            final String name = PatchIO.INVALID_HOTFIX_STRING;
            Category inv = null;
            for (ModelElement c : root.getElements()) {
                if (c instanceof Category && ((Category) c).getName().equals(name)) {
                    inv = (Category) c;
                }
            }
            if (inv == null) {
                inv = new Category(name);
                inv.setParent(root);
                root.addElement(inv, 0);
            }
            Comment x = new Comment(s);
            x.setParent(inv);
            inv.addElement(x);
        }

        @Override
        protected CompletePatch parse(BufferedReader br, String filename) throws IOException {
            CompletePatch res = new CompletePatch();
            res.setPatchSource(CompletePatch.PatchSource.FT);
            String line = removeGarbageCharacters(br.readLine());
            boolean containsProfileData = false;
            if (line.contains("<profile = ")) {
                containsProfileData = true;
                final String start = "<profile = ";
                int index = line.indexOf(start);
                while (index != -1) {
                    String actualProfile = line.substring(index + start.length(), line.indexOf(">", index));
                    res.createNewProfile(actualProfile);
                    index = line.indexOf(start, index + 1);
                }
                final String start2 = "<CurrentProfile = ";
                index = line.indexOf(start2);
                String name = line.substring(index + start2.length(), line.indexOf(">", index + 1));
                res.setCurrentProfile(name);
                do {
                    line = removeGarbageCharacters(br.readLine());
                } while (line != null && line.trim().isEmpty());
            }
            OLD_PARSE = !containsProfileData;
            if (!containsProfileData) {
                res.createNewProfile("default");
            }
            Category c2;
            String name = isStartOfNewCategory(line);
            if (name != null) {
                c2 = new Category(name);
                line = removeGarbageCharacters(br.readLine());
            } else {
                c2 = new Category(Category.DEFAULT_ROOT_NAME);
            }
            Category currentCat = c2;
            res.setRoot(c2);
            while (line != null) {
                currentCat = addLine(currentCat, line, res);
                line = removeGarbageCharacters(br.readLine());
            }

            c2 = postProcessParse(c2, res);
            res.setRoot(c2);
            inferType(res);
            return res;
        }

        /**
         * Checks if the given string is the end of this category
         *
         * @param c
         * @param s
         * @return
         */
        private static boolean isTermination(Category c, String s) {
            if (!s.startsWith("#</")) {
                return false;
            }
            if (!s.contains("</") || !s.contains(">") || s.contains("<hotfix>")) {
                return false;
            }
            if (s.indexOf(">") < s.indexOf("</")) {//The rare case where a commented line contains HTML of its own
                return false;
            }
            return s.substring(s.indexOf("</") + 2, s.indexOf(">")).trim().equals(c.getName());
        }

        private static String isStartOfNewCategory(String s) {
            if (!s.startsWith("#<")) {
                return null;
            }
            if (s.getBytes()[0] == -17) {//Some weird artifact that sometimes happens when editing by hand
                s = s.substring(1);
            }
            String trim = s.substring(1).trim();
            if (trim.length() < 3) {
                return null;
            }
            if (!trim.contains("<") || !trim.contains(">")) {
                return null;
            }
            String name = s.substring(s.indexOf("<") + 1, s.indexOf(">")).trim();
            if (FORBIDDEN_CATEGORY_NAMES.contains(name)) {
                return null;
            }
            if (name.startsWith("/")) {
                return null;
            }
            if (name.endsWith("/")) {
                return null;
            }
            return name;
        }

        /**
         * Returns what category the remainder of the parsing should be in: This
         * category, a new child category, or the parent category
         *
         * @param parent
         * @param s
         * @param patch
         * @return
         */
        private static Category addLine(Category parent, String s, CompletePatch patch) {
            if (s.trim().isEmpty()) {
                return parent;
            }
            if (parent == null) {
                return null;
            }
            int hotfixnumber = -1;
            if (s.startsWith("set Transient.SparkServiceConfiguration")) {
                hotfixnumber = s.contains("Keys") ? 0 : 1;
            }
            s = s.trim();
            if (s.getBytes()[0] == -17) {//Some weird artifact that sometimes happens when editing by hand
                s = s.substring(1);
            }
            if (s.startsWith("#")) {
                String name = isStartOfNewCategory(s);
                if (name != null) {
                    if (name.equals(FT_UPDATE_STRING_TO_CHECK)) {
                        name = FT_BLCMM_SAVED_STRING;
                    }
                    Category c2 = new Category(name, s.contains("<MUT>"), false);
                    c2.setParent(parent);
                    parent.addElement(c2);
                    return c2;
                } else if (isTermination(parent, s)) {
                    parent.combineAdjecantHotfixWrappers();
                    return parent.getParent();
                } else {
                    if (s.contains("<hotfix>")) {
                        HotfixWrapper wrap = parseHotfixCode(s, parent, patch);
                        if (wrap == null) {
                            handleInvalidHotfix(patch, s);
                        } else {
                            wrap.setParent(parent);
                            parent.addElement(wrap);
                        }
                    } else {
                        ModelElement code = parseNormalCode(s, parent, patch);
                        parent.addElement(code);
                        if (hotfixnumber != -1) {
                            fixes[hotfixnumber] = s;
                            fixes2[hotfixnumber] = (SetCommand) code;
                        }
                    }
                    return parent;
                }
            } else {
                ModelElement code = parseNormalCode(s, parent, patch);
                if (hotfixnumber != -1) {
                    fixes[hotfixnumber] = s;
                    fixes2[hotfixnumber] = (SetCommand) code;
                }
                parent.addElement(code);
                return parent;
            }
        }

        /**
         * Removes the functional hotfix lines from c and it's children, and
         * introduces hotfix metadata if required.
         *
         * @param c
         * @param patch
         * @return
         */
        private static Category postProcessParse(Category c, CompletePatch patch) {
            if (fixes[0] != null && fixes[1] != null) {
                if (c.getNumberOfHotfixDescendants() == 0) {
                    introduceMeta(c, patch);
                }
                removeCodeAndEmptyParents((Category) fixes2[0].getParent(), fixes2[0]);
                removeCodeAndEmptyParents((Category) fixes2[1].getParent(), fixes2[1]);
            }
            fixes = new String[2];
            fixes2 = new SetCommand[2];
            HashSet<SetCommand> offlineCodes = new HashSet<>();
            List<String> offlines = Arrays.asList(new String[]{PatchType.OFFLINE1, PatchType.OFFLINE2, PatchType.OFFLINE3});
            for (ModelElement code : c.getElements()) {
                if (code instanceof SetCommand && offlines.contains(code.toString())) {
                    offlineCodes.add((SetCommand) code);
                    patch.setOffline(true);
                }
            }
            for (SetCommand code : offlineCodes) {
                removeCodeAndEmptyParents(c, code);
            }
            while (c.getName().equals(Category.DEFAULT_ROOT_NAME)
                    && c.getElements().size() == 1
                    && c.getElements().get(0) instanceof Category) {
                c = (Category) c.get(0);
                c.getParent().removeElement(c);
                c.setParent(null);
            }
            return c;
        }

        private static void removeCodeAndEmptyParents(Category c, ModelElement code) {
            boolean removed = c.removeElement(code);
            if (!removed) {
                for (ModelElement el : c.getElements()) {
                    if (c instanceof Category) {
                        removeCodeAndEmptyParents((Category) el, code);
                    }
                }
            } else {
                recursiveDeleteEmptyCategories(c);
            }
        }

        private static void recursiveDeleteEmptyCategories(Category c) {
            if (!c.getElements().isEmpty()) {
                return;
            }
            if (c.getParent() != null) {
                boolean a = c.getParent().removeElement(c);
                if (!a) {
                    throw new IllegalStateException();
                }
                recursiveDeleteEmptyCategories(c.getParent());
            }
        }

        /**
         * Extracts each key-value pair from the two hotfix lines. Introduces
         * these to a new Category
         *
         * @param parent
         * @param patch
         */
        private static void introduceMeta(Category parent, CompletePatch patch) {
            Category group = new Category("Hotfixes");
            String[] split1 = splitFixes(fixes[0]);
            String[] split2 = splitFixes(fixes[1]);
            for (int i = 0; i < split1.length; i++) {
                String key = split1[i];
                String value = split2[i].replace("\\\"", "\"");
                HotfixWrapper wrap = HotfixConverter.keyAndValuetoNewWrapper(key, value);
                if (wrap == null) {
                    handleInvalidHotfix(patch, "key: " + key + " ||| value: " + value);
                } else {
                    wrap.setParent(group);
                    group.addElement(wrap);
                    patch.setSelected(wrap.get(0), true);
                }
            }
            group.combineAdjecantHotfixWrappers();
            group.setParent(parent);
            parent.addElement(group);
        }

        private static String[] splitFixes(String hotfix) {
            String[] split1 = hotfix.split("\",\"");
            split1[0] = split1[0].substring(split1[0].indexOf("\"") + 1);
            split1[split1.length - 1] = split1[split1.length - 1].substring(0, split1[split1.length - 1].lastIndexOf("\""));
            return split1;
        }

        private static ModelElement parseNormalCode(String input, Category parent, CompletePatch patch) {
            if (OLD_PARSE) {
                String code;
                if (input.contains("<off>") && input.startsWith("#")) {
                    code = input.substring(1, input.indexOf("<off>"));
                } else {
                    code = input;
                }
                ModelElement el;
                if (SetCommand.isValidCommand(code)) {
                    boolean selected = !(input.startsWith("#") && input.contains("<off>"));
                    el = new SetCommand(code);
                    if (selected) {
                        ((SetCommand) el).turnOnInProfile(patch.getCurrentProfile());
                    }
                    ((SetCommand) el).profileChanged(patch.getCurrentProfile());
                } else {
                    el = new Comment(code);
                }
                el.setParent(parent);
                return el;
            } else {
                String code = input.substring(input.indexOf("<code>") + "<code>".length(), input.indexOf("</code>"));
                if (SetCommand.isValidCommand(code)) {
                    SetCommand com = new SetCommand(code);
                    com.setParent(parent);
                    extractProfileDataFromInput(input, patch, com);
                    com.profileChanged(patch.getCurrentProfile());
                    return com;
                } else {
                    Comment comment = new Comment(code);
                    comment.setParent(parent);
                    return comment;
                }
            }
        }

        protected static void extractProfileDataFromInput(String input, CompletePatch patch, SetCommand com) {
            String start = "<inProfile = ";
            int index = input.indexOf(start);
            while (index != -1) {
                String name = input.substring(index + start.length(), input.indexOf(">", index));
                index = input.indexOf(start, index + 1);
                Profile prof = patch.getProfile(name);
                if (prof != null) {
                    com.turnOnInProfile(prof);
                }
            }
        }

        public static HotfixWrapper parseHotfixCode(String input, Category parent, CompletePatch patch) {
            boolean selected = input.trim().contains("<on>");
            String key = input.substring(input.indexOf("<key>\"") + 6, input.indexOf("\"</key>"));
            String value = input.substring(input.indexOf("<value>\"") + 8, input.indexOf("\"</value>"));
            value = unescape(value);//Legacy, to handle manual edits to allow " in hotfixes
            HotfixWrapper wrap = HotfixConverter.keyAndValuetoNewWrapper(key, value);
            if (wrap == null) {
                return null;
            }
            SetCommand com = wrap.get(0);
            extractProfileDataFromInput(input, patch, com);
            if (selected) {
                com.turnOnInProfile(patch.getCurrentProfile());
            }
            com.profileChanged(patch.getCurrentProfile());
            return wrap;
        }

        /**
         * Infers the game type of the given patch, mostly by looking for
         * hotfix targets and matching based on those.  This is only ever called
         * for FilterTool style files, so we don't have to worry about the
         * fact that AoDK data wouldn't be distinguishable from BL2.
         * @param res
         */
        private static void inferType(CompletePatch res) {
            res.setType(PatchType.BL2);
            Category root = res.getRoot();
            List<HotfixWrapper> wrappers = root.listHotfixMeta();
            outer:
            for (HotfixWrapper wrap : wrappers) {
                if (wrap.getType() == HotfixType.ONDEMAND) {
                    String arg = wrap.getParameter();
                    for (PatchType t : PatchType.values()) {
                        if (t.isOnDemandInPatchType(arg)) {
                            res.setType(t);
                            return;
                        }
                    }
                }
            }
            final String[] DLCForTPS = new String[]{"baroness", "doppel", "gd_cork",
                "crocus", "enforcer", "prototype",
                "gladiator", "lawbringer", "marigold",
                "quince", "petunia", "gd_co_", "gd_ma_",
                "moonstone", "laserbuggy", "moonitems",
                "cypressure", "protowarbot"};
            final String[] DLCForTPSAtStart = new String[]{"ma_"};
            for (ModelElement element : root.listRecursiveContentMinusCategories()) {
                if (element instanceof SetCommand) {
                    SetCommand command = (SetCommand) element;
                    String object = command.getObject().toLowerCase();
                    int idx = object.indexOf(".");
                    String firstPackage = idx > 0 ? object.substring(0, idx) : object;
                    for (String s : DLCForTPS) {
                        if (firstPackage.contains(s)) {
                            res.setType(PatchType.TPS);
                            return;
                        }
                    }
                    for (String s : DLCForTPSAtStart) {
                        if (firstPackage.startsWith(s)) {
                            res.setType(PatchType.TPS);
                            return;
                        }
                    }
                }
            }
            res.setType(PatchType.BL2);
        }

    }

    private final static class BLCMMParser extends Parser {

        private static int readingVersion = 0;
        private static boolean foundHeader = false;

        /**
         * Importer for BLCMM-style files. Note that "filename" is ignored,
         * since BLCMM-saved files will always have a top-level category of some
         * sort.
         *
         * @param br The reader providing our data
         * @param filename The filename providing the data (unused)
         * @return The CompletePatch object created from the data
         * @throws IOException
         */
        @Override
        @SuppressWarnings("empty-statement")
        protected CompletePatch parse(BufferedReader br, String filename) throws IOException {
            ModelElementContainer current = null;
            CompletePatch res = new CompletePatch();
            res.setPatchSource(CompletePatch.PatchSource.BLCMM);
            String line;
            Stack<XMLTag> stack = new Stack<>();

            // We'll want to make sure that we found a proper BLCMM header tag
            // with a version number attached.  (The parsing itself will throw
            // an IllegalArgumentException if we've been told to read a file
            // with a higher version than we support.)
            foundHeader = false;

            do {
                while ((line = removeGarbageCharacters(br.readLine())).isEmpty());//concise code ftw

                if (line.equals(FT_UPDATE_STRING)) {
                    continue;
                }
                Line split = new Line(line);
                current = addLine(split, res, current, stack);
            } while (line != null && !stack.isEmpty());

            // Make sure that we've found a valid BLCMM header (I think that we'd end up
            // having an Exception long before we get here, actually.)
            if (!foundHeader) {
                throw new IllegalArgumentException("BLCMM header tag not found!");
            }

            return res;
        }

        private static ModelElementContainer addLine(Line split, CompletePatch res, ModelElementContainer current, Stack<XMLTag> stack) {
            final boolean fixMissingProfiles = true;
            int idx = 0;
            while (split.elements.size() > idx) {
                Object element = split.elements.get(idx);
                if (element instanceof XMLTag) {
                    XMLTag tag = (XMLTag) element;
                    if (!tag.single && !tag.end) {//open something
                        stack.push(tag);
                        switch (tag.name) {
                            case "category":
                                Category category = new Category(tag.arguments.get("name"), tag.arguments.containsKey("MUT"), tag.arguments.containsKey("locked"));
                                category.setParent(current);
                                if (current == null) {//This will be the root
                                    res.setRoot(category);
                                } else {
                                    current.addElement(category);
                                }
                                current = category;
                                break;
                            case "hotfix":
                                HotfixType type = HotfixType.PATCH;
                                String param = null;
                                if (tag.arguments.containsKey("level")) {
                                    type = HotfixType.LEVEL;
                                    param = tag.arguments.get("level");
                                } else if (tag.arguments.containsKey("package")) {
                                    type = HotfixType.ONDEMAND;
                                    param = tag.arguments.get("package");
                                }
                                HotfixWrapper wrapper = new HotfixWrapper(tag.arguments.get("name"), type, param);
                                wrapper.setParent(current);
                                current.addElement(wrapper);
                                current = wrapper;
                                break;
                            case "code":
                                idx++;
                                StringBuilder combuilder = new StringBuilder();
                                Object o = split.elements.get(idx);
                                while (!(o instanceof XMLTag && ((XMLTag) o).name.equals("code"))) {
                                    combuilder.append(o.toString());
                                    o = split.elements.get(++idx);
                                }
                                String com = combuilder.toString();
                                SetCommand command = com.startsWith("set ") ? (current instanceof HotfixWrapper ? new HotfixCommand(com) : new SetCommand(com)) : new SetCMPCommand(com);
                                command.setParent(current);
                                current.addElement(command);
                                if (tag.arguments.containsKey("profiles")) {
                                    String profiles = tag.arguments.get("profiles");
                                    String[] profs = profiles.split(",");
                                    for (String prof : profs) {
                                        Profile p = res.getProfile(prof);
                                        if (p != null) {
                                            command.turnOnInProfile(p);
                                        } else if (fixMissingProfiles && !(prof.isEmpty() && profs.length == 1)) {
                                            p = res.createNewProfile(prof);
                                            command.turnOnInProfile(p);
                                        }
                                    }
                                } else {//legacy
                                    boolean sel = !tag.arguments.containsKey("selected");
                                    if (sel) {
                                        command.turnOnInProfile(res.getCurrentProfile());
                                    }
                                }
                                command.profileChanged(res.getCurrentProfile());
                                idx--;
                                break;
                            case "comment":
                                idx++;
                                combuilder = new StringBuilder();
                                o = split.elements.get(idx);
                                while (!(o instanceof XMLTag && ((XMLTag) o).name.equals("comment"))) {
                                    combuilder.append(o.toString());
                                    o = split.elements.get(++idx);
                                }
                                Comment comment = new Comment(combuilder.toString());
                                comment.setParent(current);
                                current.addElement(comment);
                                idx--;
                                break;
                            case "BLCMM":
                                if (tag.arguments.containsKey("v")) {
                                    try {
                                        readingVersion = Integer.parseInt(tag.arguments.get("v"));
                                        if (readingVersion > SAVE_VERSION) {
                                            throw new IllegalArgumentException(String.format(
                                                    "File is BLCMMv%d, we can only open up to v%d",
                                                    readingVersion, SAVE_VERSION));
                                        } else if (readingVersion < 1) {
                                            throw new IllegalArgumentException(String.format(
                                                    "Invalid BLCMM file version number specified: %d",
                                                    readingVersion));
                                        } else {
                                            foundHeader = true;
                                        }
                                    } catch (NumberFormatException e) {
                                        throw new IllegalArgumentException("Unable to parse BLCMM file version: " + tag.toString());
                                    }
                                } else {
                                    throw new IllegalArgumentException("Improperly-formed BLCMM file.  Version not found: " + tag.toString());
                                }
                                break;
                            case "profiles":
                            case "head":
                            case "body":
                                //do nothing
                                break;
                            default:
                                throw new IllegalArgumentException(tag.toString());
                        }
                    } else if (!tag.single && tag.end) { //close something
                        XMLTag pop = stack.pop();
                        if (!pop.name.equalsIgnoreCase(tag.name)) {
                            throw new IllegalStateException("Unexpected XML tag: " + tag + " was expecting the closing tag of " + pop + "\nCurrent stack:\n" + Arrays.toString(stack.toArray()).replace(",", "\n").replaceAll("[\\[\\]]", ""));
                        }
                        switch (tag.name) {
                            case "category":
                            case "hotfix":
                                current = current.getParent();
                                break;
                            case "head"://do nothing
                                if (res.getProfiles().isEmpty()) {
                                    res.createNewProfile("default");
                                }
                                break;
                            case "body"://do nothing
                            case "profiles"://do nothing
                            case "BLCMM":
                            case "code":
                            case "comment":
                                break;
                            default:
                                throw new IllegalArgumentException(tag.toString());
                        }
                    } else {
                        assert tag.single;
                        switch (tag.name) {
                            case "type":
                                res.setType(PatchType.valueOf(tag.arguments.get("name").toUpperCase()));
                                res.setOffline(Boolean.parseBoolean(tag.arguments.get("offline")));
                                break;
                            case "profile":
                                String name = tag.arguments.get("name");
                                res.profiles.put(name, new Profile(name));
                                if (tag.arguments.containsKey("current")) {
                                    res.setCurrentProfile(name);
                                }
                                break;
                            default:
                                throw new IllegalArgumentException(tag.toString());
                        }
                    }
                } else if (element instanceof String) {
                    throw new IllegalArgumentException();//should be handled by the wrapping XML case
                } else {
                    throw new IllegalArgumentException();
                }
                idx++;
            }
            return current;
        }

        private static class XMLTag {

            String name;
            HashMap<String, String> arguments = new HashMap<>();
            boolean single;
            boolean end;

            XMLTag(String tag) {
                if (!tag.startsWith("<")) {
                    throw new IllegalArgumentException(tag);
                }
                if (!tag.endsWith(">")) {
                    throw new IllegalArgumentException(tag);
                }
                end = tag.startsWith("</");
                single = tag.endsWith("/>");
                int spaceidx = tag.indexOf(" ");
                name = spaceidx == -1 ? tag.substring(end ? 2 : 1, tag.length() - (single ? 2 : 1)) : tag.substring(end ? 2 : 1, spaceidx);
                while (spaceidx > 0) {
                    int eqidx = tag.indexOf("=", spaceidx);
                    int begqidx = tag.indexOf("\"", eqidx);
                    int endqidx = tag.indexOf("\"", begqidx + 1);
                    while (tag.charAt(endqidx - 1) == '\\') {
                        endqidx = tag.indexOf("\"", endqidx + 1);
                    }
                    String arg = tag.substring(spaceidx + 1, eqidx).trim();
                    String val = unescape(tag.substring(begqidx + 1, endqidx));

                    arguments.put(arg, val);
                    spaceidx = tag.indexOf(" ", endqidx);
                }
            }

            @Override
            public String toString() {
                StringBuilder args = new StringBuilder();
                for (String arg : arguments.keySet()) {
                    args.append(" ").append(arg).append("=\"").append(arguments.get(arg)).append("\"");
                }
                return String.format("<%s%s%s%s>", end ? "/" : "", name, args.toString(), single ? "/" : "");
            }

        }

        private static class Line {

            private final List<Object> elements = new ArrayList<>();

            private Line(String line) {
                line = line.trim();
                int idx = 0;
                int tag = -1;
                while (idx < line.length()) {
                    if (line.charAt(idx) == '<' && lookaheadForTag(line, idx) != -1) {//An xml tag
                        if (tag == -1) {
                            int idx2 = idx + 1;
                            boolean inquotes = false;
                            while (true) {
                                char c = line.charAt(idx2);
                                idx2++;
                                if (c == '>' && !inquotes) {
                                    break;
                                } else if (c == '"') {
                                    if (!inquotes) {
                                        inquotes = true;
                                    } else if (idx2 > 1 && line.charAt(idx2 - 2) != '\\') {//>1 and -2 because idx2 has been incremented at this point
                                        inquotes = false;
                                    }
                                }
                            }
                            elements.add(new XMLTag(line.substring(idx, idx2)));
                            idx = idx2;
                        } else {
                            elements.add(new XMLTag(line.substring(idx, tag + 1)));
                            idx = tag + 1;
                            tag = -1;
                        }
                    } else {//inside an xml tag
                        int idx2 = idx;
                        boolean inquotes = false;
                        int lastStart = -1;
                        while (true) {//sanning until we hit the start of the next xml tag
                            if (idx2 == line.length()) {
                                if (lastStart != -1) {
                                    idx2 = lastStart;
                                    break;
                                } else {
                                    throw new IllegalArgumentException(line);
                                }
                            }
                            char c = line.charAt(idx2);
                            if (c == '<') {
                                tag = lookaheadForTag(line, idx2);
                                if (tag != -1) {
                                    if (!inquotes) {
                                        break;
                                    } else {
                                        lastStart = idx2;
                                    }
                                }
                            } else if (c == '"') {
                                inquotes = !inquotes;
                            }
                            idx2++;
                        }
                        elements.add(line.substring(idx, idx2));
                        idx = idx2;
                    }
                }
            }

            private static int lookaheadForTag(String line, int idx2) {
                assert line.charAt(idx2) == '<';
                int idx3 = idx2 + 1;
                boolean lookingForArguments = false;
                boolean lookingForValue = false;
                boolean hasletter = false;
                while (idx3 < line.length()) {
                    char c = line.charAt(idx3);
                    if (!lookingForArguments) {
                        if (c == '>' && idx3 - idx2 > 1) {
                            return idx3;
                        }
                        if (c == ' ') {
                            lookingForArguments = true;
                        } else if (!Character.isAlphabetic(c) && !(idx3 - idx2 == 1 && c == '/')) {
                            return -1;
                        }
                    } else {
                        if (lookingForValue) {
                            if (c == '"' && line.charAt(idx3 - 1) != '\\') {
                                lookingForValue = false;
                                hasletter = false;
                            }
                        } else {
                            if (c == '=') {
                                if (line.charAt(idx3 + 1) == '"') {
                                    idx3++;
                                    lookingForValue = true;
                                } else {
                                    return -1;
                                }
                            } else if (c == '>' && !hasletter) {
                                return idx3;
                            } else if (c == '/' && line.charAt(idx3 + 1) == '>' && !hasletter) {
                                idx3++;
                                return idx3;
                            } else if (c == ' ' && !hasletter) {//skip
                            } else if (!Character.isAlphabetic(c)) {
                                return -1;
                            } else {
                                hasletter = true;
                            }
                        }
                    }
                    idx3++;
                }
                return -1;
            }
        }
    }

    //The methods below are for saving
    /**
     * Only to be used for debugging purposes. Write immediately to files when
     * exporting.
     *
     * @param patch
     * @return
     */
    public static String toParseString(ModelElement patch) {
        StringWriter writer = new StringWriter();
        try {
            writeStructure(patch, writer, "", LINEBREAK);
            writer.close();
            return writer.getBuffer().toString();
        } catch (IOException ex) {
            Logger.getLogger(PatchIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Writes out a complete patch/mod to a Writer.  The mod will be saved slightly
     * differently if the `exporting` boolean is set -- namely, some messages about
     * importing mods to OpenBLCMM will be added in for exported mods.
     *
     * @param patch The patchset to save
     * @param writer The writer to write to
     * @param exporting Whether or not we're exporting
     * @return A list of Strings to be shown to the user, if possible
     * @throws IOException
     */
    public static List<String> writeToFile(CompletePatch patch, Writer writer, boolean exporting) throws IOException {
        return writeToFile(patch, SaveFormat.BLCMM, writer, exporting);
    }
    /**
     * Writes out a complete patch/mod to a Writer.  The mod will be saved slightly
     * differently if the `exporting` boolean is set -- namely, some messages about
     * importing mods to OpenBLCMM will be added in for exported mods.  This version
     * supports supplying a SaveFormat, but currently only BLCMM output is supported.
     *
     * @param patch The patchset to save
     * @param format The save format to use
     * @param writer The writer to write to
     * @param exporting Whether or not we're exporting
     * @return A list of Strings to be shown to the user, if possible
     * @throws IOException
     */

    public static List<String> writeToFile(CompletePatch patch, SaveFormat format, Writer writer, boolean exporting) throws IOException {

        // Enforce what kinds of formats we support (ie: at the moment, only BLCMM)
        if (format != SaveFormat.BLCMM) {
            throw new IllegalArgumentException("Only BLCMM saving is supported currently");
        }

        // Enforce offline status from settings
        patch.setOffline(Options.INSTANCE.getSaveAsOffline());
        Category root = patch.getRoot();
        PatchType type = patch.getType();

        // BLCMM format (the only one we now support)
        writer.append(String.format("<BLCMM v=\"%s\">\n" + FT_UPDATE_STRING + "\n\t<head>\n\t\t<type name=\"%s\" offline=\"%s\"/>\n".replace("\n", LINEBREAK),
                SAVE_VERSION + "", type.name(), patch.isOffline()));
        if (patch.profiles.size() > 0) {
            writer.append(patch.getprofileXML());//save the profiles
        }
        writer.append("\t</head>\n\t<body>\n".replace("\n", LINEBREAK));
        writeStructure(root, writer);
        writer.append("\t</body>\n</BLCMM>\n\n".replace("\n", LINEBREAK));

        Category newCommands = new Category("");
        HashSet<ModelElement> excludes = new HashSet<>();
        List<String> res = new ArrayList<>(analyzeLevelMerges(type, root, excludes, newCommands));
        if (newCommands.size() > 0) {
            writer.append("#Level merges:" + LINEBREAK);
            writeFunctionalCodes(newCommands, writer, new HashSet<>());
            writer.append(LINEBREAK);
        }
        writer.append("#Commands:" + LINEBREAK);
        writeFunctionalCodes(root, writer, excludes);

        if (exporting && root.getNumberOfHotfixDescendants() > 0) {
            writer.append(LINEBREAK);
            writer.append("#Direct-Execute Warning:" + LINEBREAK);
            writer.append(String.format(
                    "say WARNING: \"%s\" must be imported into " + Meta.NAME + " to run "
                    + "properly with UCP or other mods.\n",
                    root.getName()).replace("\n", LINEBREAK));
        }

        if (patch.getProfiles().size() > 1) {
            writer.append(LINEBREAK);
            writer.append("#Profile notice:" + LINEBREAK);
            writer.append(String.format(
                    "say Executed your mod with the following profile: %s.\n",
                    patch.getCurrentProfile().getName()).replace("\n", LINEBREAK));
        }

        writer.append(LINEBREAK);
        if (root.getNumberOfHotfixDescendants() > 0) {
            writer.append("#Hotfixes:" + LINEBREAK);
            Category GBXFixes = getGBXFixes(type);
            writeFunctionalHotfix(GBXFixes, root, type, writer, patch.isOffline());//Only write hotfix data when hotfixes are present
        }
        return res;
    }

    private static void writeStructure(Category root, Writer writer) throws IOException {
        writeStructure(root, writer, "\t\t", LINEBREAK);
    }

    private static void writeStructure(ModelElement el, Writer writer, String prefix, String postfix) throws IOException {
        if (el instanceof ModelElementContainer) {
            ModelElementContainer<ModelElement> root = (ModelElementContainer) el;
            writer.append(prefix + root.getXMLStringPrefix() + postfix);
            for (ModelElement c : root.getElements()) {
                writeStructure(c, writer, prefix + "\t", postfix);
            }
            writer.append(prefix + root.getXMLStringPostfix() + postfix);
        } else {
            writer.append(prefix + el.toXMLString() + postfix);
        }
    }

    private static void writeFunctionalHotfix(Category gbx, Category root, PatchType type, Writer writer, boolean offline) throws IOException {
        File f = File.createTempFile("temp_hotfixes", "temp");
        BufferedWriter valuewriter = new BufferedWriter(new FileWriter(f));
        OSInfo.OS OS = GameDetection.getVirtualOS(type);

        List<HotfixWrapper> hotfixes = gbx.listHotfixMeta();
        hotfixes.addAll(root.listHotfixMeta());
        writer.append(type.getFunctionalHotfixPrefix(offline, LINEBREAK));
        HotfixConverter conv = new HotfixConverter();
        int i = 0;
        HashSet<String> illegalValues = new HashSet<>();
        final int numberOfGBXHotfixes = gbx.getNumberOfHotfixDescendants();
        for (HotfixWrapper wrapper : hotfixes) {
            for (SetCommand command : wrapper.getElements()) {
                boolean gbxFix = i++ < numberOfGBXHotfixes;//notice the increment here
                if (command.isSelected()) {
                    HotfixConverter.HotfixKeyValuePair kvp = conv.getKeyValuePair((HotfixCommand)command);
                    if (!illegalValues.contains(kvp.value)) {
                        if (i > 1) {
                            writer.append(",");
                            valuewriter.append(",");
                        }
                        writer.append("\"" + kvp.key + "\"");
                        valuewriter.append("\"" + escape(kvp.value) + "\"");
                    }
                    if (gbxFix) {
                        illegalValues.add(kvp.value);
                    }
                }
            }
        }
        valuewriter.close();
        writer.append(type.getFunctionalHotfixCenter(offline, LINEBREAK).replace("\n\n", LINEBREAK));
        FileReader fis = new FileReader(f);
        char[] buffer = new char[8 * 1024];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            writer.write(buffer, 0, bytesRead);
        }
        fis.close();
        f.delete();
        writer.append(type.getFunctionalHotfixPostfix(offline, LINEBREAK));
        //Now we're doing 3x as much IO as needed compared to storing the values in memory
        //We could do the main loop twice, so we have 1x IO but 2x CPU usage
        //Or actually store it in memory and risk RAM issues
        //Or store it in RAM if small, and stream to file if it gets too big
        //TODO revise maybe?
        //Can also enhance the HotfixConverter API to allow for some ease of use
        //If we shift blcmm.model to the utilities JAR, the converter has access to the
        //HotfixContainer and other elements, which it could then provide custom-tailored calls for,
        //which would simplify this code, but that means moving the model out of the main project.
        //And I (LightChaosman) prefer to keep "volatile" code in the main project, until it's stabilized to a final form
    }

    private static void writeFunctionalCodes(Category root, Writer writer, HashSet<ModelElement> excludes) throws IOException {
        for (ModelElement element : root.getElements()) {
            if (excludes.contains(element)) {
                continue;
            }
            if (element instanceof Category) {
                writeFunctionalCodes((Category) element, writer, excludes);
            } else if (element instanceof SetCommand && ((SetCommand) element).isSelected()) {
                writer.append(((SetCommand) element).getCode() + LINEBREAK);
            } else if (element instanceof Comment) {
                TransientModelData transientData = element.getTransientData();
                if (false
                        || transientData.getNumberOfOccurences(GlobalListOfProperties.CommentChecker.Exec.class) > 0
                        || transientData.getNumberOfOccurences(GlobalListOfProperties.CommentChecker.Say.class) > 0
                        || transientData.getNumberOfOccurences(GlobalListOfProperties.IncompleteSetCommandChecker.class) > 0) {
                    writer.append(((Comment) element).toString() + LINEBREAK);
                }
            }
            //if hotfixwrapper, ignore
        }
    }

    static Category getGBXFixes(PatchType type) {
        return getStoredCommands(type, "GBXFIXES", "GBX_hotfixes.blcm");
    }

    private static Category getVanillaLevelList(PatchType type) {
        return getStoredCommands(type, "Level lists", "vanillaLevelLists.blcm");
    }

    private static Category getStoredCommands(PatchType type, String categoryName, String filename) {
        InputStream hotfixStream = ClassLoader.getSystemClassLoader().getResourceAsStream("resources/" + type.name() + "/" + filename);
        Category res = new Category(categoryName);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(hotfixStream));) {
            CompletePatch p = new BLCMMParser().parse(br, "");
            res = p.getRoot();
        } catch (Exception ex) {//TODO report to user? :thinking: What would user do with this information... if resources.jar is present, this never fails.
            GlobalLogger.log(ex);
        }
        return res;
    }

    static List<String> analyzeLevelMerges(PatchType type, Category toBeChecked, HashSet<ModelElement> excludes, Category newCommands) {
        List<String> res = new ArrayList<>();
        HashSet<SetCommand> newExcludes = new HashSet<>();
        Map<String, Collection<SetCommand>> levelMerges = new LinkedHashMap<>();
        Map<String, SetCommand> vanillamerges = null;
        Profile p = new Profile("");
        SetCommand currentCommand = null;
        try {
            analyzeCategoryForLevelMerges(toBeChecked, levelMerges);
            for (String object : levelMerges.keySet()) {
                Collection<SetCommand> coms = levelMerges.get(object);
                LevelDepArray<LevelDepStruct> vanillaArray = null;
                for (SetCommand com : coms) {
                    currentCommand = com;
                    if (!com.isSelected()) {
                        continue;
                    } else if (vanillaArray == null) {
                        if (vanillamerges == null) {
                            Category vmerges = getVanillaLevelList(type);
                            vanillamerges = new HashMap<>();
                            for (ModelElement el : vmerges.getElements()) {
                                SetCommand vcom = (SetCommand) el;
                                vanillamerges.put(vcom.getObject(), vcom);
                            }
                        }
                        // The cast here could throw an exception, but just let it get handled below
                        vanillaArray = (LevelDepArray) LevelDepParser.parse(vanillamerges.get(object).getValue());
                    }
                    String val2 = com.getValue();
                    // This cast too could throw an exception.  Also let it get handled below.
                    LevelDepArray<LevelDepStruct> userArray = (LevelDepArray) LevelDepParser.parse(val2);
                    for (int i = 0; i < userArray.size(); i++) {
                        String userPersistent = userArray.get(i).getString("PersistentMap");
                        LevelDepArray<LevelDepString> userSecondaries = userArray.get(i).getArray("SecondaryMaps");
                        for (int j = 0; j < vanillaArray.size(); j++) {
                            String vanillaPersistent = vanillaArray.get(j).getString("PersistentMap");
                            LevelDepArray<LevelDepString> vanillaSecondaries = vanillaArray.get(j).getArray("SecondaryMaps");
                            if (userPersistent.equalsIgnoreCase(vanillaPersistent)) {
                                for (int k = 0; k < userSecondaries.size(); k++) {
                                    LevelDepString newsec = userSecondaries.get(k);
                                    // A null value can happen if the user has an extra comma in there
                                    if (newsec != null) {
                                        boolean present = false;
                                        for (int l = 0; l < vanillaSecondaries.size(); l++) {
                                            LevelDepString oldsec = vanillaSecondaries.get(l);
                                            if (oldsec.equalsIgnoreCase(newsec)) {
                                                present = true;
                                            }
                                        }
                                        if (!present) {
                                            vanillaSecondaries.add(newsec);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (vanillaArray != null) {
                    SetCommand newcom = new SetCommand(object, "LevelList", vanillaArray.toString());
                    newcom.setParent(newCommands);
                    newcom.turnOnInProfile(p);
                    newcom.profileChanged(p);
                    newCommands.addElement(newcom);
                }
                newExcludes.addAll(coms);
            }
        } catch (Exception e) {
            newExcludes.clear();
            newCommands.clear();
            GlobalLogger.log("Error during level merges: ");
            GlobalLogger.log(e);
            String reportCommand = "";
            if (currentCommand != null) {
                reportCommand = "<br/>" + Utilities.CodeFormatter.deFormatCodeForUserDialog(currentCommand.getCode());
            }
            res.add("<Html>"
                    + "The following map merging statement is in unexpected format.<br/>"
                    + "Map merging statements have not been combined!<br/>"
                    + reportCommand
            );
        }
        excludes.addAll(newExcludes);
        return res;
    }

    private static void analyzeCategoryForLevelMerges(Category toBeChecked, Map<String, Collection<SetCommand>> levelMerges) {
        for (ModelElement el : toBeChecked.getElements()) {
            if (el instanceof Category) {
                analyzeCategoryForLevelMerges((Category) el, levelMerges);
            } else if (el instanceof SetCommand) {
                SetCommand com = (SetCommand) el;
                if (com.getField().toLowerCase().equals("levellist")) {
                    levelMerges.putIfAbsent(com.getObject(), new ArrayList<>());
                    levelMerges.get(com.getObject()).add(com);
                }
            } else {
                //skip - irrelevant (e.g. HotfixContainer)
            }
        }
    }
}
