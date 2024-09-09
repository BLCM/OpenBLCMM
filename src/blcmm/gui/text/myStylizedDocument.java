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
package blcmm.gui.text;

import blcmm.gui.theme.ThemeManager;
import java.awt.Color;
import java.util.HashSet;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public class myStylizedDocument extends DefaultStyledDocument {

    private final DefaultStyledDocument doc;
    private final Element rootElement;
    private final MutableAttributeSet normal;
    private final MutableAttributeSet keyword;
    private final MutableAttributeSet GDword;
    private final MutableAttributeSet MTword;
    private final MutableAttributeSet number;
    private final MutableAttributeSet quotedouble;
    private final MutableAttributeSet quote;
    private final HashSet<String> keywords;

    public myStylizedDocument() {
        doc = this;
        rootElement = doc.getDefaultRootElement();
        putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");

        // Text
        normal = new SimpleAttributeSet();
        StyleConstants.setForeground(normal, ThemeManager.getColor(ThemeManager.ColorType.CodeText));

        // Keywords
        keyword = new SimpleAttributeSet();
        StyleConstants.setForeground(keyword, ThemeManager.getColor(ThemeManager.ColorType.CodeKeyword));
        StyleConstants.setBold(keyword, true);

        // GD_* words
        GDword = new SimpleAttributeSet();
        StyleConstants.setForeground(GDword, ThemeManager.getColor(ThemeManager.ColorType.CodeGDWord));

        // MT_* words
        MTword = new SimpleAttributeSet();
        StyleConstants.setForeground(MTword, ThemeManager.getColor(ThemeManager.ColorType.CodeMTWord));

        // Numbers
        number = new SimpleAttributeSet();
        StyleConstants.setForeground(number, ThemeManager.getColor(ThemeManager.ColorType.CodeNumber));

        // Double quotes
        quotedouble = new SimpleAttributeSet();
        StyleConstants.setForeground(quotedouble, ThemeManager.getColor(ThemeManager.ColorType.CodeDoubleQuote));

        // Single quotes
        quote = new SimpleAttributeSet();
        StyleConstants.setForeground(quote, ThemeManager.getColor(ThemeManager.ColorType.CodeSingleQuote));

        // Set up keywords
        keywords = new HashSet<>();
        keywords.add("True");
        keywords.add("False");
        keywords.add("None");
        keywords.add("set");
        keywords.add("set_cmp");
        keywords.add("say");
        keywords.add("exec");
    }

    /*
     * Override to apply syntax highlighting after the document has been updated
     */
    @Override
    public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
        super.insertString(offset, str, a);
        processChangedLines(offset, str.length());
    }

    /*
     * Override to apply syntax highlighting after the document has been updated
     */
    @Override
    public void remove(int offset, int length) throws BadLocationException {
        super.remove(offset, length);
        processChangedLines(offset, 0);
    }

    /*
     * Determine how many lines have been changed,
     * then apply highlighting to each line
     */
    private void processChangedLines(int offset, int length) throws BadLocationException {
        String content = doc.getText(0, doc.getLength());
        // The lines affected by the latest document update
        int startLine = rootElement.getElementIndex(offset);
        int endLine = rootElement.getElementIndex(offset + length);
        // Do the actual highlighting
        for (int i = startLine; i <= endLine; i++) {
            applyHighlighting(content, i);
        }
    }

    /*
     * Parse the line to determine the appropriate highlighting
     */
    private void applyHighlighting(String content, int line) throws BadLocationException {
        int startOffset = rootElement.getElement(line).getStartOffset();
        int endOffset = rootElement.getElement(line).getEndOffset() - 1;
        int lineLength = endOffset - startOffset + 1;
        int contentLength = content.length();
        if (endOffset >= contentLength) {
            endOffset = contentLength - 1;
        }

        // set normal attributes for the line
        //System.out.println(startOffset + " " + lineLength);
        if (lineLength < 1) {
            return;//this line breaks undo+newlins TODO
        }
        doc.setCharacterAttributes(startOffset, lineLength, normal, true);

        // check for single line comment
        /*int index = content.indexOf(getSingleLineDelimiter(), startOffset);
        if ((index > -1) && (index < endOffset)) {
            doc.setCharacterAttributes(index, endOffset - index + 1, comment, false);
            endOffset = index - 1;
        }*/
        // check for tokens
        checkForTokens(content, startOffset, endOffset);
    }

    /*
     * Parse the line for tokens to highlight
     */
    private void checkForTokens(String content, int startOffset, int endOffset) {
        while (startOffset <= endOffset) {
            // skip the delimiters to find the start of a new token
            while (isDelimiter(content.substring(startOffset, startOffset + 1))) {
                if (startOffset < endOffset) {
                    startOffset++;
                } else {
                    return;
                }
            }
            // Extract and process the entire token
            if (isDoubleQuoteDelimiter(content.substring(startOffset, startOffset + 1))) {
                startOffset = getQuoteToken(content, startOffset, endOffset);
            } else if (startsWithCustomColorAndContainsEnd(content.substring(startOffset, endOffset))) {
                startOffset = getEndOfColorToken(content, startOffset, endOffset);
            } else {
                startOffset = getOtherToken(content, startOffset, endOffset);
            }
        }
    }

    /*
     * Parse the line to get the quotes and highlight it
     */
    private int getQuoteToken(String content, int startOffset, int endOffset) {
        String quoteDelimiter = content.substring(startOffset, startOffset + 1);
        String escapeString = getEscapeString(quoteDelimiter);
        int index;
        int endOfQuote = startOffset;
        // skip over the escape quotes in this quote
        index = content.indexOf(escapeString, endOfQuote + 1);
        while ((index > -1) && (index < endOffset)) {
            endOfQuote = index + 1;
            index = content.indexOf(escapeString, endOfQuote);
        }
        // now find the matching delimiter
        index = content.indexOf(quoteDelimiter, endOfQuote + 1);
        if ((index < 0) || (index > endOffset)) {
            endOfQuote = endOffset;
        } else {
            endOfQuote = index;
        }
        doc.setCharacterAttributes(startOffset, endOfQuote - startOffset + 1, quotedouble, false);
        return endOfQuote + 1;
    }

    private int getOtherToken(String content, int startOffset, int endOffset) {
        int endOfToken = startOffset + 1;
        while (endOfToken <= endOffset) {
            if (isDelimiter(content.substring(endOfToken, endOfToken + 1))) {
                break;
            }
            endOfToken++;
        }
        String token = content.substring(startOffset, endOfToken);
        if (isKeyword(token)) {
            doc.setCharacterAttributes(startOffset, endOfToken - startOffset, keyword, false);
        } else if (token.toLowerCase().startsWith("gd_") || startsWithCapitalsAndUnderscores(token)) {
            doc.setCharacterAttributes(startOffset, endOfToken - startOffset, GDword, false);
        } else if (isSpecialValue(token)) {
            doc.setCharacterAttributes(startOffset, endOfToken - startOffset, MTword, false);
        } else {
            try {
                Double.parseDouble(token);
                doc.setCharacterAttributes(startOffset, endOfToken - startOffset, number, false);
            } catch (NumberFormatException e) {
            }
        }
        if (token.contains("'")) {
            int index = token.indexOf("'");
            int endindex = token.indexOf("'", index + 1);

            if (endindex != -1) {
                String clazz = token.substring(0, index);
                String rest = token.substring(index + 1, endindex);
                doc.setCharacterAttributes(startOffset + index, endindex - index + 1, quote, false);
                if (!clazz.equals("Class") && !clazz.equals("Package") && clazz.length() > 0) {
                    MutableAttributeSet link2 = new SimpleAttributeSet();
                    link2.addAttribute("URL", token);
                    StyleConstants.setUnderline(link2, true);
                    doc.setCharacterAttributes(startOffset + index + 1, endindex - index - 1, link2, false);
                }
            } else {
                //System.out.println(token  + " "+ endOfToken + " " + endOffset + " " + content.substring(endOfToken, endOfToken + 1) + isDelimiter(content.substring(endOfToken, endOfToken + 1)));
            }

        }
        return endOfToken + 1;
    }

    /*
     * Override for other languages
     */
    protected boolean isDelimiter(String character) {
        String operands = ";{}()[]+/%=!&|^~*,";
        return Character.isWhitespace(character.charAt(0)) || operands.contains(character);
    }

    /*
     * Override for other languages
     */
    protected boolean isDoubleQuoteDelimiter(String character) {
        return character.equals("\"");
    }

    /*
     * Override for other languages
     */
    protected boolean isKeyword(String token) {
        return keywords.contains(token);
    }

    /*
     * Override for other languages
     */
    protected String getEscapeString(String quoteDelimiter) {
        return "\\" + quoteDelimiter;
    }

    private static boolean startsWithCustomColorAndContainsEnd(String substring) {
        if (substring == null) {
            return false;
        }
        final String regex = "<font[ ]+color[ ]*=[ ]*\"#[0-9a-fA-F]{6}[ ]*\">.*</font>.*";//for simplicity we won't allow trailing spaces in the </font> tag.
        return substring.matches(regex);
    }

    private int getEndOfColorToken(String content, int startOffset, int endOffset) {
        int idxHashtag = content.indexOf("#", startOffset), idxFirstCloseBrace = content.indexOf(">", startOffset);
        String color = content.substring(idxHashtag, idxFirstCloseBrace - 1);

        int endOfContent = content.indexOf("</font>", startOffset + 1);
        SimpleAttributeSet atts = new SimpleAttributeSet();
        StyleConstants.setForeground(atts, Color.decode(color));
        StyleConstants.setBold(atts, true);
        doc.setCharacterAttributes(idxFirstCloseBrace + 1, endOfContent - (idxFirstCloseBrace + 1),
                atts, false);
        //Explicitly color the quotes in the font tag, since we color the entire font tag in this method, it won't get caught by the rest of the parser.
        int q1 = content.indexOf("\"", startOffset);
        int q2 = content.indexOf("\"", q1 + 1);
        doc.setCharacterAttributes(q1, q2 - q1 + 1, quotedouble, false);

        return endOfContent + "</font>".length();//Continue parsing *after* the end of the </font> tag

    }

    private static boolean isSpecialValue(String token) {
        if (!token.contains("_")) {
            return false;
        }
        String[] split = token.split("_");
        if (split.length != 2 || split[0].length() < 2 || token.contains(".")) {
            return false;
        }
        for (int i = 0; i < split[0].length(); i++) {
            if (!Character.isUpperCase(split[0].charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWithCapitalsAndUnderscores(String token) {
        if (!token.contains(".")) {
            return false;
        }
        String[] split = token.split("\\.");
        if (split.length == 0) {
            return false;
        }
        String start = split[0];
        for (char c : start.toCharArray()) {
            if (!(Character.isUpperCase(c) || c == '_')) {
                return false;
            }
        }
        return !token.startsWith(".");
    }

}
