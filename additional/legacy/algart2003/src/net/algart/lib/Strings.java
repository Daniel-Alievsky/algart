/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001-2003 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.lib;

import java.util.*;

/**
 * <p>Library of tools for working with strings.
 *
 * @author  Daniel Alievsky
 * @version 1.1
 */

public final class Strings {

    /**
     * Don't let anyone instantiate this class.
     */
    private Strings() {}

    public static boolean between(int a, int b, int c) {
        /* Duplicated in Mat */
        return b <= c ? a >= b && a <= c : a >= c && a <= b;
    }

    public static boolean between(int a, char[] bounds) {
        /* Analog of Mat.between() */
        for (int k = 0; k <= bounds.length - 2; k += 2) {
            if (between(a, bounds[k], bounds[k + 1]))
                return true;
        }
        return false;
    }

    public static String substring(String s, int beginIndex, int endIndex) {
        try {
            int len = s.length();
            if (beginIndex < 0)
                beginIndex = 0;
            if (endIndex > len)
                endIndex = len;
            return s.substring(beginIndex, endIndex);
        } catch (NullPointerException e) {
            return "";
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public static String substr(String s, int beginIndex, int len) {
        return substring(s, beginIndex, beginIndex + len);
    }

    public static String substring(String s, int beginIndex) {
        try {
            if (beginIndex < 0)
                beginIndex = 0;
            return s.substring(beginIndex);
        } catch (NullPointerException e) {
            return "";
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public static char charAt(String s, int off) {
        try {
            return s.charAt(off);
        } catch (NullPointerException e) {
            return 0;
        } catch (IndexOutOfBoundsException e) {
            return 0;
        }
    }

    /** Returns s if any string is null, or s is "", or oldSubstring is ""
     */
    public static final int UNIX_LINES = java.util.regex.Pattern.UNIX_LINES;
    public static final int CASE_INSENSITIVE = java.util.regex.Pattern.CASE_INSENSITIVE;
    public static final int COMMENTS = java.util.regex.Pattern.COMMENTS;
    public static final int MULTILINE = java.util.regex.Pattern.MULTILINE;
    public static final int DOTALL = java.util.regex.Pattern.DOTALL;
    public static final int UNICODE_CASE = java.util.regex.Pattern.UNICODE_CASE;
    public static final int CANON_EQ = java.util.regex.Pattern.CANON_EQ;
    public static boolean find(String s, String regex) {
        return find(s, regex, 0);
    }

    public static boolean find(String s, String regex, int flags) {
        return java.util.regex.Pattern.compile(regex, flags).matcher(s).find();
    }

    public static String replaceAll(String s, String regex, String replacement, int flags) {
        return java.util.regex.Pattern.compile(regex, flags).matcher(s).replaceAll(replacement);
    }

    public static int indexOf(String s, char c1, char c2) {
        return indexOf(s, 0, c1, c2);
    }

    public static int indexOf(String s, int fromIndex, char c1, char c2) {
        if (s == null)
            return -1;
        int len = s.length();
        for (int k = fromIndex; k < len; k++)
            if (Ma.between(s.charAt(k), c1, c2))
                return k;
        return -1;
    }

    public static int indexOf(String s, char[] bounds) {
        return indexOf(s, 0, bounds);
    }

    public static int indexOf(String s, int fromIndex, char[] bounds) {
        if (s == null)
            return -1;
        int len = s.length();
        for (int k = fromIndex; k < len; k++)
            if (between(s.charAt(k), bounds))
                return k;
        return -1;
    }

    public static int lastIndexOf(String s, char c1, char c2) {
        return s == null ? -1 : lastIndexOf(s, s.length() - 1, c1, c2);
    }

    public static int lastIndexOf(String s, int fromIndex, char c1, char c2) {
        if (s == null)
            return -1;
        for (int k = fromIndex; k >= 0; k--)
            if (Ma.between(s.charAt(k), c1, c2))
                return k;
        return -1;
    }

    public static int lastIndexOf(String s, char[] bounds) {
        return s == null ? -1 : lastIndexOf(s, s.length() - 1, bounds);
    }

    public static int lastIndexOf(String s, int fromIndex, char[] bounds) {
        if (s == null)
            return -1;
        for (int k = fromIndex; k >= 0; k--)
            if (between(s.charAt(k), bounds))
                return k;
        return -1;
    }

    public static String toIgnoreCase(String s) {
        return s.toLowerCase().toUpperCase();
    }

    public static String untilLastSubstring(String s, String sub) {
        return s.substring(0, s.lastIndexOf(sub) + 1);
    }

    public static String beforeLastSubstring(String s, String sub) {
        int p = s.lastIndexOf(sub);
        if (p == -1)
            return s;
        return s.substring(0, p);
    }

    public static String afterLastSubstring(String s, String sub) {
        return s.substring(s.lastIndexOf(sub) + 1);
    }

    public static String dup(char c, int len) {
        char[] ca = new char[len];
        for (int k = 0; k < len; k++)
            ca[k] = c;
        return new String(ca);
    }

    public static String dup(String s, int n) {
        int len = s.length();
        StringBuffer sb = new StringBuffer(n * len);
        for (int k = 0; k < n; k++)
            sb.append(s);
        return sb.toString();
    }

    public static String leftPad(String s, int len) {
        return leftPad(s, len, ' ');
    }

    public static String rightPad(String s, int len) {
        return rightPad(s, len, ' ');
    }

    public static String leftPad(String s, int len, char pad) {
        return s.length() > len ? s : dup(pad, len - s.length()) + s;
    }

    public static String rightPad(String s, int len, char pad) {
        return s.length() > len ? s : s + dup(pad, len - s.length());
    }

    public static String leftTrim(String s) {
        int len = s.length();
        for (int p = 0; p < len; p++) {
            if (s.charAt(p) > ' ')
                return s.substring(p);
        }
        return "";
    }

    public static String rightTrim(String s) {
        int len = s.length();
        for (int p = len - 1; p >= 0; p--) {
            if (s.charAt(p) > ' ')
                return s.substring(0, p + 1);
        }
        return "";
    }

    public static int charCount(String s, char c) {
        int result = 0;
        for (int k = 0, len = s.length(); k < len; k++) {
            if (s.charAt(k) == c)
                result++;
        }
        return result;
    }

    public static String addLFIfNotEmpty(String s) {
        if (s == null || s.length() == 0)
            return "";
        return s + "\n";
    }

    public static String appendSeparator(String s, String separator) {
        if (s == null || s.length() == 0)
            return "";
        if (s.endsWith(separator))
            return s;
        return s + separator;
    }

    /** Removes ending '\n' character or "\r\n" character pair;
     * also converts null to "".
     */
    public static String chomp(String s) {
        int len;
        if (s == null || (len = s.length()) == 0)
            return "";
        if (s.charAt(len - 1) != '\n')
            return s;
        len--;
        if (len > 0 && s.charAt(len - 1) == '\r')
            len--;
        return s.substring(0, len);
    }

    public static String chop(String s) {
        if (s == null || s.length() == 0)
            return "";
        return s.substring(0, s.length() - 1);
    }

    public static String normalizeLF(String s) {
        return normalizeLF(s, "\n");
    }

    public static String normalizeLF(String s, String newLineSeparator) {
        return s.replaceAll("\\r\\n?|\\n", newLineSeparator);
    }

    public static String encodeUnicodeEscapesSingle(String s) {
        int len = s.length();
        StringBuffer sb = new StringBuffer(len);
        for (int k = 0; k < len; k++) {
            char c = s.charAt(k);
            if (c == '\\' && k + 6 <= len && s.charAt(k + 1) == 'u') {
                sb.append("\\uu"); k++;
            } else if (c >= 128) {
                sb.append("\\u" + leftPad(Integer.toHexString(c).toUpperCase(), 4, '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static char[] encodeUnicodeEscapesSingle(char[] s) {
        return encodeUnicodeEscapesSingle(s, 0, s.length);
    }

    public static char[] encodeUnicodeEscapesSingle(char[] s, int off, int len) {
        StringBuffer sb = new StringBuffer(len);
        len += off;
        for (int k = off; k < len; k++) {
            char c = s[k];
            if (c == '\\' && k + 6 <= len && s[k + 1] == 'u') {
                sb.append("\\uu"); k++;
            } else if (c >= 128) {
                sb.append("\\u" + leftPad(Integer.toHexString(c).toUpperCase(), 4, '0'));
            } else {
                sb.append(c);
            }
        }
        char[] result = new char[sb.length()];
        sb.getChars(0, result.length, result, 0);
        return result;
    }

    public static String decodeUnicodeEscapesSingle(String s) {
        return decodeUnicodeEscapes(s, true);
    }

    public static String decodeUnicodeEscapes(String s) {
        return decodeUnicodeEscapes(s, false);
    }

    public static String decodeUnicodeEscapes(String s, boolean justDeleteFirstUInSequenceOfSeveralU) {
        int len = s.length();
        StringBuffer sb = new StringBuffer(len);
        for (int k = 0; k < len; k++) {
            char c = s.charAt(k);
            if (c == '\\' && k + 6 <= len && s.charAt(k + 1) == 'u') {
                int q = k + 2;
                while (q + 4 <= len && s.charAt(q) == 'u')
                    q++;
                if (justDeleteFirstUInSequenceOfSeveralU && q > k + 2) {
                    k++; // skipping 1 'u' character
                } else
                    try {
                        c = (char)Integer.parseInt(s.substring(q, q + 4), 16);
                        k = q + 3; // saving unchanged k on exception
                    } catch (NumberFormatException e) {
                    }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static char[] decodeUnicodeEscapesSingle(char[] s) {
        return decodeUnicodeEscapes(s, true);
    }

    public static char[] decodeUnicodeEscapes(char[] s) {
        return decodeUnicodeEscapes(s, false);
    }

    public static char[] decodeUnicodeEscapes(char[] s, boolean justDeleteFirstUInSequenceOfSeveralU) {
        return decodeUnicodeEscapes(s, 0, s.length, justDeleteFirstUInSequenceOfSeveralU);
    }

    public static char[] decodeUnicodeEscapesSingle(char[] s, int off, int len) {
        return decodeUnicodeEscapes(s, off, len, true);
    }

    public static char[] decodeUnicodeEscapes(char[] s, int off, int len) {
        return decodeUnicodeEscapes(s, off, len, false);
    }

    public static char[] decodeUnicodeEscapes(char[] s, int off, int len, boolean justDeleteFirstUInSequenceOfSeveralU) {
        StringBuffer sb = new StringBuffer(len);
        len += off;
        for (int k = off; k < len; k++) {
            char c = s[k];
            if (c == '\\' && k + 6 <= len && s[k + 1] == 'u') {
                int q = k + 2;
                while (q + 4 <= len && s[q] == 'u')
                    q++;
                if (justDeleteFirstUInSequenceOfSeveralU && q > k + 2) {
                    k++; // skipping 1 'u' character
                } else
                    try {
                        c = (char)Integer.parseInt(new String(s, q, 4), 16);
                        k = q + 3; // saving unchanged k on exception
                    } catch (NumberFormatException e) {
                    }
            }
            sb.append(c);
        }
        char[] result = new char[sb.length()];
        sb.getChars(0, result.length, result, 0);
        return result;
    }

    public static String ctrlsToJava(String s) {
        int len = s.length();
        StringBuffer sb = new StringBuffer(len);
        for (int k = 0; k < len; k++) {
            char c = s.charAt(k);
            switch (c) {
                case '\\':
                    sb.append("\\\\"); break;
                case '\n':
                    sb.append("\\n"); break;
                case '\r':
                    sb.append("\\r"); break;
                case '\b':
                    sb.append("\\b"); break;
                case '\t':
                    sb.append("\\t"); break;
                case '\f':
                    sb.append("\\f"); break;
                case '"':
                    sb.append("\\\""); break;
                case '\'':
                    sb.append("\\\'"); break;
                default:
                    sb.append(c); break;
            }
        }
        return sb.toString();
    }

    public static String javaToCtrls(String s) {
        int len = s.length();
        StringBuffer sb = new StringBuffer(len);
        for (int k = 0; k < len; k++) {
            char c = s.charAt(k);
            if (c == '\\' && k + 1 < len) {
                char next = s.charAt(k + 1);
                char d2, d3;
                if (next == '\\') {
                    sb.append('\\'); k++;
                } else if (next == 'n') {
                    sb.append('\n'); k++;
                } else if (next == 'r') {
                    sb.append('\r'); k++;
                } else if (next == 'b') {
                    sb.append('\b'); k++;
                } else if (next == 't') {
                    sb.append('\t'); k++;
                } else if (next == 'f') {
                    sb.append('\f'); k++;
                } else if (next == '"') {
                    sb.append('\"'); k++;
                } else if (next == '\'') {
                    sb.append('\''); k++;
                } else if (k + 3 < len
                    && next >= '0' && next <= '9'
                    && ((d2 = s.charAt(k + 2)) >= '0') && d2 <= '9'
                    && ((d3 = s.charAt(k + 3)) >= '0') && d3 <= '9') {
                    sb.append((char)((next - '0') * 64 + (d2 - '0') * 8 + d3 - '0')); k += 3;
                } else
                    sb.append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String ctrlsToXml(String s) {
        return ctrlsToXml(s, false, false);
    }

    public static String ctrlsToXml(String s, boolean encodeForAttributes) {
        return ctrlsToXml(s, encodeForAttributes, false);
    }

    public static String ctrlsToXml(String s, boolean encodeForAttributes, boolean replaceLowCtrlsToSpace) {
        int len = s.length();
        StringBuffer sb = new StringBuffer(len);
        for (int k = 0; k < len; k++) {
            char c = s.charAt(k);
            switch (c) {
                case '&':
                    sb.append("&amp;"); break;
                case '<':
                    sb.append("&lt;"); break;
                case '>':
                    sb.append("&gt;"); break;
                default:
                    if (encodeForAttributes) {
                        if (c == '"') {
                            sb.append("&quot;");
                        } else if (c < ' ' || c >= 0xFFFE) {
                            if (replaceLowCtrlsToSpace)
                                sb.append(' ');
                            else
                                sb.append("&#" + leftPad(Integer.toString(c), 2, '0') + ";");
                        } else {
                            sb.append(c);
                        }
                    } else {
                        if ((c < ' ' && !(c == '\n' || c == '\r' || c == '\t')) || c >= 0xFFFE) {
                            if (replaceLowCtrlsToSpace)
                                sb.append(' ');
                            else
                                sb.append("&#" + leftPad(Integer.toString(c), 2, '0') + ";");
                        } else {
                            sb.append(c);
                        }
                    }
                    break;
            }
        }
        return sb.toString();
    }

    public static String xmlToCtrls(String s) {
        StringBuffer sb = new StringBuffer(s.length());
        return xmlToCtrls(s, 0, s.length(), sb).toString();
    }

    public static String xmlToCtrls(String s, int start, int end) {
        StringBuffer sb = new StringBuffer();
        return xmlToCtrls(s, start, end, sb).toString();
    }

    public static StringBuffer xmlToCtrls(String s, StringBuffer sb) {
        return xmlToCtrls(s, 0, s.length(), sb);
    }

    public static StringBuffer xmlToCtrls(String s, int start, int end, StringBuffer sb) {
        if (sb == null)
            return null;
        for (int k = start; k < end; k++) {
            char c = s.charAt(k);
            if (c == '&' && k + 3 < end) {
                String sub = s.substring(k + 1, Ma.min(k + 6, end));
                char d1, d2;
                if (sub.startsWith("amp;")) {
                    sb.append('&'); k += 4;
                } else if (sub.startsWith("lt;")) {
                    sb.append('<'); k += 3;
                } else if (sub.startsWith("gt;")) {
                    sb.append('>'); k += 3;
                } else if (sub.startsWith("quot;")) {
                    sb.append('"'); k += 5;
                } else if (sub.charAt(0) == '#' && k + 4 < end && sub.charAt(3) == ';'
                    && ((d1 = sub.charAt(1)) >= '0') && d1 <= '9'
                    && ((d2 = sub.charAt(2)) >= '0') && d2 <= '9') {
                    sb.append((char)((d1 - '0') * 10 + d2 - '0')); k += 4;
                } else
                    sb.append(c);
            } else {
                sb.append(c);
            }
        }
        return sb;
    }

    public static String xmlAttr(String attrName, String value) {
        if (attrName == null || value == null || attrName.length() == 0 || value.length() == 0)
            return "";
        return " " + attrName + "=\"" + ctrlsToXml(value, true) + "\"";
    }

    public static String xmlTag(String tagName, String text) {
        return xmlTag(tagName, text, true);
    }

    public static String xmlTag(String tagName, String text, boolean doEncodeText) {
        if (tagName == null || text == null || tagName.length() == 0 || text.length() == 0)
            return "";
        return "<" + tagName + ">" + (doEncodeText ? ctrlsToXml(text) : text) + "</" + tagName + ">";
    }

    public static String htmlPreformat(String html, boolean enableWordWrap) {
        return htmlPreformat(html, 8, enableWordWrap, true, false);
    }

    public static String htmlPreformat(String ascii,
        int tabLen,
        boolean enableWordWrap,
        boolean addMonospaceFontTag,
        boolean addHtmlTag) {
        ascii = Out.deTab(normalizeLF(ascii), tabLen);
        String prefix =
            (addHtmlTag ? "<html><body>" : "")
            + (addMonospaceFontTag ? "<font face=\"monospace\">" : "");
        String postfix =
            (addMonospaceFontTag ? "</font>" : "")
            + (addHtmlTag ? "</body></html>" : "");
        if (!enableWordWrap) {
            return prefix + replaceAll(
                replaceAll(ascii, "\\n", "<br>", DOTALL),
                " ", "&nbsp;", DOTALL) + postfix;
        }
        int len = ascii.length();
        StringBuffer sb = new StringBuffer(
            len + prefix.length() + postfix.length());
        sb.append(prefix);
        int k = 0;
        for (; k < len && ascii.charAt(k) == ' '; k++)
            sb.append("&nbsp;");
        for (; k < len; k++) {
            char c = ascii.charAt(k);
            if (c == '\n') {
                sb.append("<br>");
                for (; k + 1 < len && ascii.charAt(k + 1) == ' '; k++)
                    sb.append("&nbsp;");
            } else if (c == ' ') {
                for (; k + 1 < len && ascii.charAt(k + 1) == ' '; k++)
                    sb.append("&nbsp;");
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        sb.append(postfix);
        return sb.toString();
    }

    public static final int BY_ROWS = 0x00;
    public static final int BY_COLUMNS = 0x01;
    public static final int HTML_TAG = 0x02;
    public static final int HTML_AUTO_SIZE = 0x02 + 0x04;
    public static String htmlTable(Object[][] cellValues) {
        return htmlTable(cellValues, null, null, 0);
    }

    public static String htmlTable(Object[][] cellValues, int flags) {
        return htmlTable(cellValues, null, null, flags);
    }

    public static String htmlTable(Object[][] cellValues, String tableAttributes) {
        return htmlTable(cellValues, tableAttributes, null, 0);
    }

    public static String htmlTable(Object[][] cellValues, String tableAttributes, int flags) {
        return htmlTable(cellValues, tableAttributes, null, flags);
    }

    public static String htmlTable(Object[][] cellValues, String tableAttributes, String[] tdAttributes) {
        return htmlTable(cellValues, tableAttributes, tdAttributes, 0);
    }

    public static String htmlTable(Object[][] cellValues,
        String tableAttributes, String[] tdAttributes, int flags) {
        StringBuffer sb = new StringBuffer();
        if ((flags & HTML_TAG) != 0) {
            sb.append("<html");
            if ((flags & HTML_AUTO_SIZE) != 0)
                sb.append(" width=\"*\" height=\"*\"");
            sb.append(">");
        }
        sb.append("<table ");
        if (tableAttributes != null)
            sb.append(tableAttributes);
        else
            sb.append("cellpadding=2 cellspacing=0 border=1");
        sb.append(">\n");
        if ((flags & BY_COLUMNS) != 0) {
            for (int i = 0, len = cellValues[0].length; i < len; i++) {
                sb.append("  <tr>\n");
                for (int j = 0; j < cellValues.length; j++) {
                    if (i > 0 && (i >= cellValues[j].length || cellValues[j][i] == null))
                        continue;
                    int rowspan = 1;
                    while (i + rowspan < len && (i + rowspan >= cellValues[j].length
                        || cellValues[j][i + rowspan] == null))
                        rowspan++;
                    sb.append("    <td");
                    if (tdAttributes != null && j < tdAttributes.length && tdAttributes[j] != null)
                        sb.append(" ").append(tdAttributes[j]);
                    if (rowspan > 1)
                        sb.append(" ").append("rowspan=").append(rowspan);
                    sb.append(">");
                    if (i > 0 || (i < cellValues[j].length && cellValues[j][i] != null))
                        sb.append(cellValues[j][i]);
                    sb.append("</td>\n");
                }
                sb.append("  </tr>\n");
            }
        } else {
            for (int i = 0; i < cellValues.length; i++) {
                sb.append("  <tr>\n");
                for (int j = 0, len = cellValues[i].length, colspan = 1; j < len; j += colspan) {
                    colspan = 1;
                    while (j + colspan < len && cellValues[i][j + colspan] == null)
                        colspan++;
                    sb.append("    <td");
                    if (tdAttributes != null && j < tdAttributes.length && tdAttributes[j] != null)
                        sb.append(" ").append(tdAttributes[j]);
                    if (colspan > 1)
                        sb.append(" ").append("colspan=").append(colspan);
                    sb.append(">");
                    if (cellValues[i][j] != null)
                        sb.append(cellValues[i][j]);
                    sb.append("</td>\n");
                }
                sb.append("  </tr>\n");
            }
        }
        sb.append("</table>");
        if ((flags & HTML_TAG) != 0)
            sb.append("</html>");
        return sb.toString();
    }

    public static class NextInfo {
        public String value;
        public int index;
        public NextInfo(int index, String value) {
            this.value = value;
            this.index = index;
        }
    }

    public static NextInfo findNextQuote(char[] s, int start) {
        if (start >= s.length)
            return null;
        char c = s[start];
        if (c != '"' && c != '\'')
            return null;
        return findNextQuote(s, start, c);
    }

    public static NextInfo findNextQuote(String s, int start) {
        if (start >= s.length())
            return null;
        char c = s.charAt(start);
        if (c != '"' && c != '\'')
            return null;
        return findNextQuote(s, start, c);
    }

    public static NextInfo findNextQuote(char[] s, int start, char quote) {
        StringBuffer sb = new StringBuffer();
        int p = start;
        int len = s.length;
        if (p >= len || s[p++] != quote)
            return null;
        char c;
        while (p < len && (c = s[p]) != quote) {
            p++;
            if (c == '\\' && p < len) {
                switch (c = s[p]) {
                    case '\\':
                        c = '\\'; break;
                    case 'n':
                        c = '\n'; break;
                    case 'r':
                        c = '\r'; break;
                    case 'b':
                        c = '\b'; break;
                    case 't':
                        c = '\t'; break;
                    case 'f':
                        c = '\f'; break;
                    case '\'':
                        c = '\''; break;
                    case '"':
                        c = '"'; break;
                }
                p++;
            }
            sb.append(c);
        }
        return new NextInfo(p, sb.toString());
    }

    public static NextInfo findNextQuote(String s, int start, char quote) {
        StringBuffer sb = new StringBuffer();
        int p = start;
        int len = s.length();
        if (p >= len || s.charAt(p++) != quote)
            return null;
        char c;
        while (p < len && (c = s.charAt(p)) != quote) {
            p++;
            if (c == '\\' && p < len) {
                switch (c = s.charAt(p)) {
                    case '\\':
                        c = '\\'; break;
                    case 'n':
                        c = '\n'; break;
                    case 'r':
                        c = '\r'; break;
                    case 'b':
                        c = '\b'; break;
                    case 't':
                        c = '\t'; break;
                    case 'f':
                        c = '\f'; break;
                    case '\'':
                        c = '\''; break;
                    case '"':
                        c = '"'; break;
                }
                p++;
            }
            sb.append(c);
        }
        return new NextInfo(p, sb.toString());
    }

    public static NextInfo findSubstringIgnoringQuotedStart(String s, String match) {
        return findSubstringIgnoringQuotedStart(s, match, 0);
    }

    public static NextInfo findSubstringIgnoringQuotedStart(String s, String match, int start) {
        NextInfo result = findNextQuote(s, start);
        if (result == null) {
            if (match == null)
                return new NextInfo(s.length(), s.substring(start));
            int index = s.indexOf(match, start);
            if (index == -1)
                return null;
            return new NextInfo(index, s.substring(start, index));
        }
        if (match == null)
            return result;
        result.index = s.indexOf(match, result.index + 1);
        if (result.index == -1)
            return null;
        return result;
    }

    public static String endSubstringIgnoringQuotedStart(String s, int start) {
        return findSubstringIgnoringQuotedStart(s, null, start).value;
    }

    public static String[] split(String s) {
        return split(s, " \t\n\r\f");
    }

    public static String[] split(String s, String separators) {
        return (String[])splitAsList(s, separators).toArray(new String[0]);
    }

    public static List splitAsList(String s, String separators) {
        ArrayList result = new ArrayList();
        if (s == null)
            return result;
        StringTokenizer stok = new StringTokenizer(s, separators);
        for (; stok.hasMoreTokens(); ) {
            result.add(stok.nextToken());
        }
        return result;
    }

    public static String[] splitStringWithQuotes(String s,
        char separator // examples: ; , | \n
        ) {
        // Example: splitStringWithQuotes(" 'Some \\'\\nstring; ';; ;\"\"; 123",';')
        // returns {"Some '\nstring; ", null, null, "", "123"}
        // Both " and ' can be used in any token.
        // If some token starts from " or ', all its content after closing
        // quote is ignored:
        //   splitStringWithQuotes(" ' Some ' sss; ",';') returns {" Some ",null), though
        //   splitStringWithQuotes(" Some sss; ",';') returns {"Some sss",null);
        // Also:
        //   splitStringWithQuotes("",';') returns empty array, but
        //   splitStringWithQuotes(";",';') returns {null,null}.
        List tokens = new ArrayList();
        splitStringWithQuotes(s, separator, tokens);
        String[] result = new String[tokens.size()];
        for (int k = 0; k < result.length; k++)
            result[k] = (String)tokens.get(k);
        return result;
    }

    public static void splitStringWithQuotes(String s,
        char separator,
        List resultStrings) {
        int len = s.length();
        char[] cs = s.toCharArray();
        boolean first = true;
        for (int p = 0; true; first = false) {
            char c = 0;
            while (p < len && (c = cs[p]) <= ' ')
                p++;
            if (p < len && (c == '"' || c == '\'')) {
                NextInfo info = findNextQuote(cs, p, c);
                p = info.index;
                resultStrings.add(info.value.toString());
                while (p < len && cs[p] != separator)
                    p++;
            } else {
                int pstart = p;
                while (p < len && cs[p] != separator)
                    p++;
                int pend = p;
                while (pend > pstart && cs[pend - 1] <= ' ')
                    pend--;
                String token = null;
                if (pend > pstart)
                    token = new String(cs, pstart, pend - pstart);
                else if (first && p >= len)
                    break;
                resultStrings.add(token);
            }
            if (p >= len)
                break;
            p++;
        }
    }

    public static String javaNameToTagName(String name) {
        return javaNameToWords(name, "-");
    }

    public static String javaNameToWords(String name, String separator) {
        StringBuffer sb = new StringBuffer();
        char chLast = 0;
        for (int k = 0; k < name.length(); k++) {
            char ch = name.charAt(k);
            if (Character.isLetter(ch) && Character.isUpperCase(ch)
                && (k == 0 || Character.isLetter(chLast) || Character.isDigit(chLast))) {
                if (k > 0)
                    sb.append(separator);
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
            chLast = ch;
        }
        return sb.toString();
    }
}
