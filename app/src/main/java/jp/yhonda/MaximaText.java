/*
    Copyright 2012, 2013, 2014, 2015, 2016, 2017 Yasuaki Honda (yasuaki.honda@gmail.com)
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MaximaOnAndroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package jp.yhonda;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text processing between the input field, Maxima and the web views. Plain
 * Java without Android dependencies, so that it is covered by JVM unit tests.
 */
final class MaximaText {
    private static final Pattern OLD_MANUAL_URL =
            Pattern.compile("maxima-doc/(?:en/)?([A-Za-z_]+)/maxima\\.html$");
    private static final Pattern MBOX_VERB =
            Pattern.compile("\\\\\\\\mbox\\{\\\\\\\\verb\\|(.)\\|\\}");

    private MaximaText() {
    }

    /**
     * Terminates a command with ';' unless its last non-blank character is
     * already ';' or '$'. Trailing blanks are removed.
     */
    static String terminateCommand(String cmd) {
        int i = cmd.length() - 1;
        char c = ';';
        while (i >= 0) {
            c = cmd.charAt(i);
            if (c == ' ' || c == '\t') {
                i--;
            } else {
                break;
            }
        }
        if (c == ';' || c == '$') {
            return cmd.substring(0, i + 1);
        } else {
            return cmd.substring(0, i + 1) + ';';
        }
    }

    /**
     * Extracts the input commands from a manual example such as
     * "(%i1) expand((x+y)^2);\n(%o1) ...\n(%i2) factor(%);" -- one entry per
     * (%iN) line, cut after its last ';' or '$'.
     */
    static String[] exampleCommands(String example) {
        String[] parts = example.split("\n\\(%");
        parts[0] = parts[0].replaceFirst("^\\(%", "");
        List<String> commands = new ArrayList<String>();
        for (String part : parts) {
            part = part.replaceAll("\n", " ");
            if (!part.startsWith("i")) {
                continue;
            }
            String cmd = part.substring(part.indexOf(" ") + 1);
            int end = cmd.lastIndexOf(";");
            if (end <= 0) {
                end = cmd.lastIndexOf("$");
            }
            if (end > 0) {
                cmd = cmd.substring(0, end + 1); // keep the terminator
            }
            commands.add(cmd);
        }
        return commands.toArray(new String[0]);
    }

    /** Escapes single quotes for a JavaScript string literal in single quotes. */
    static String escapeQuotes(String text) {
        return text.replace("'", "\\'");
    }

    /** Replaces every occurrence of pattern (literally). */
    static String substitute(String input, String pattern, String replacement) {
        return input.replace(pattern, replacement);
    }

    /**
     * Inside \mbox{...}, turns line breaks into "}\\ \mbox{" so that a
     * multi-line text box becomes one box per line (with the backslashes
     * doubled, as the result goes into a JavaScript string literal).
     */
    static String splitMboxLines(String tex) {
        StringBuilder result = new StringBuilder();
        String rest = tex;
        int p;
        while ((p = rest.indexOf("mbox{")) != -1) {
            result.append(rest, 0, p).append("mbox{");
            int close = rest.indexOf("}", p + 5);
            if (close < 0) { // unbalanced: keep the rest unchanged
                return result.append(rest.substring(p + 5)).toString();
            }
            result.append(substitute(rest.substring(p + 5, close), "\n",
                    "}\\\\\\\\ \\\\mbox{"));
            rest = rest.substring(close);
        }
        return result.append(rest).toString();
    }

    /**
     * Merges a run of \\mbox{\\verb|c|} (one character each, as Maxima's tex
     * produces for strings) into a single \\text{...}.
     */
    static String mergeVerbChars(String tex) {
        Matcher m = MBOX_VERB.matcher(tex);
        if (!m.find()) {
            return tex;
        }
        StringBuffer sb = new StringBuffer();
        m.appendReplacement(sb, "\\\\\\\\text{$1");
        while (m.find()) {
            m.appendReplacement(sb, "$1");
        }
        m.appendTail(sb);
        return sb.toString() + "}";
    }

    /**
     * Maps a manual URL stored by MoA 3.x (maxima-doc/[en/]<lang>/maxima.html)
     * to the current layout (maxima-doc/<lang>/index.html); null if the URL
     * needs no change.
     */
    static String migratedManualUrl(String url) {
        Matcher m = OLD_MANUAL_URL.matcher(url);
        if (!m.find()) {
            return null;
        }
        return "file:///android_asset/maxima-doc/" + m.group(1) + "/index.html";
    }
}
