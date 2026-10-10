package jp.yhonda;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MaximaTokenizerTest {
    private final MaximaTokenizer tokenizer = new MaximaTokenizer();

    @Test
    public void tokenStartsAfterDelimiter() {
        String text = "integrate(sin";
        assertEquals("integrate(".length(), tokenizer.findTokenStart(text, text.length()));
    }

    @Test
    public void tokenStartSkipsLeadingBlanks() {
        String text = "x+  fac";
        assertEquals("x+  ".length(), tokenizer.findTokenStart(text, text.length()));
    }

    @Test
    public void tokenStartAtBeginningOfInput() {
        assertEquals(0, tokenizer.findTokenStart("expand", 6));
    }

    @Test
    public void tokenEndsAtDelimiter() {
        assertEquals(3, tokenizer.findTokenEnd("sin+cos", 0));
        assertEquals(6, tokenizer.findTokenEnd("factor", 0));
    }

    @Test
    public void tokenEndsAtOpeningParenthesis() {
        // '(' is the first character of the delimiter list.
        assertEquals(3, tokenizer.findTokenEnd("sin(x)", 0));
    }

    @Test
    public void terminateTokenAddsNothing() {
        assertEquals("expand", tokenizer.terminateToken("expand").toString());
    }
}
