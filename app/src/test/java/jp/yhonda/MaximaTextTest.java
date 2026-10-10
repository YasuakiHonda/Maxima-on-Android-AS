package jp.yhonda;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MaximaTextTest {

    @Test
    public void terminateCommandAddsSemicolon() {
        assertEquals("2+2;", MaximaText.terminateCommand("2+2"));
        assertEquals("2+2;", MaximaText.terminateCommand("2+2  \t"));
    }

    @Test
    public void terminateCommandKeepsExistingTerminator() {
        assertEquals("a:1$", MaximaText.terminateCommand("a:1$"));
        assertEquals("f(x);", MaximaText.terminateCommand("f(x);   "));
    }

    @Test
    public void exampleCommandsTakesInputLinesOnly() {
        String example = "(%i1) expand((x+y)^2);\n"
                + "                          2            2\n"
                + "(%o1)                    y  + 2 x y + x\n"
                + "(%i2) factor(%);\n"
                + "(%o2)                       (y + x)";
        assertArrayEquals(new String[] {"expand((x+y)^2);", "factor(%);"},
                MaximaText.exampleCommands(example));
    }

    @Test
    public void exampleCommandsJoinsContinuationLinesAndKeepsDollar() {
        String example = "(%i1) a: [1,\n2, 3]$\n(%i2) length(a);\n(%o2) 3";
        assertArrayEquals(new String[] {"a: [1, 2, 3]$", "length(a);"},
                MaximaText.exampleCommands(example));
    }

    @Test
    public void exampleCommandsWithoutInputLines() {
        assertArrayEquals(new String[0], MaximaText.exampleCommands("(%o1) 42"));
    }

    @Test
    public void escapeQuotesForJavaScript() {
        assertEquals("diff(f(x),x,1) \\' and \\'", MaximaText.escapeQuotes("diff(f(x),x,1) ' and '"));
    }

    @Test
    public void substituteReplacesAllOccurrences() {
        assertEquals("a<br>b<br><br>c", MaximaText.substitute("a\nb\n\nc", "\n", "<br>"));
        assertEquals("unchanged", MaximaText.substitute("unchanged", "\n", "<br>"));
    }

    @Test
    public void substituteHandlesLongOutput() {
        // The previous recursive implementation used one stack frame per match.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            sb.append("x\n");
        }
        String result = MaximaText.substitute(sb.toString(), "\n", "<br>");
        assertEquals(200000 * "x<br>".length(), result.length());
    }

    @Test
    public void splitMboxLinesBreaksMultiLineBoxes() {
        // The inserted break has its backslashes doubled: the TeX is then put
        // into a JavaScript string literal.
        assertEquals("\\mbox{ab}\\\\\\\\ \\\\mbox{cd} + x",
                MaximaText.splitMboxLines("\\mbox{ab\ncd} + x"));
        assertEquals("x^2", MaximaText.splitMboxLines("x^2"));
    }

    @Test
    public void splitMboxLinesToleratesUnclosedBox() {
        assertEquals("\\mbox{ab\ncd", MaximaText.splitMboxLines("\\mbox{ab\ncd"));
    }

    @Test
    public void mergeVerbCharsJoinsSingleCharacters() {
        // As it arrives from Maxima's tex for a string, backslashes doubled.
        String tex = "\\\\mbox{\\\\verb|a|}\\\\mbox{\\\\verb|b|}\\\\mbox{\\\\verb|c|}";
        assertEquals("\\\\text{abc}", MaximaText.mergeVerbChars(tex));
        assertEquals("x+1", MaximaText.mergeVerbChars("x+1"));
    }

    @Test
    public void migratedManualUrlMapsMoA3Layout() {
        String base = "file:///android_asset/maxima-doc/";
        assertEquals(base + "en/index.html", MaximaText.migratedManualUrl(base + "en/maxima.html"));
        assertEquals(base + "de/index.html", MaximaText.migratedManualUrl(base + "en/de/maxima.html"));
        assertEquals(base + "pt_BR/index.html", MaximaText.migratedManualUrl(base + "en/pt_BR/maxima.html"));
        assertEquals(base + "ja/index.html", MaximaText.migratedManualUrl(base + "ja/maxima.html"));
    }

    @Test
    public void migratedManualUrlLeavesCurrentUrlsAlone() {
        assertNull(MaximaText.migratedManualUrl("file:///android_asset/maxima-doc/es/index.html"));
        assertNull(MaximaText.migratedManualUrl("file:///android_asset/maxima-doc/en/Integration.html"));
    }
}
