package jp.yhonda;

import android.widget.MultiAutoCompleteTextView;

/**
 * Created by yasube on 2017/03/08.
 */

public class MaximaTokenizer implements MultiAutoCompleteTextView.Tokenizer {
    String delimiter = "()[],.;:+*/-=<>`!#$%&'^";
    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;
        while (i > 0 && delimiter.indexOf(text.charAt(i - 1))== -1) {
            i--;
        }
        while (i < cursor && text.charAt(i) == ' ') {
            i++;
        }
        return i;
    }
    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();
        while (i < len) {
            if (delimiter.indexOf(text.charAt(i)) >0) {
                return i;
            } else {
                i++;
            }
        }
        return len;
    }
    public CharSequence terminateToken(CharSequence text) {
        return text;
    }
}
