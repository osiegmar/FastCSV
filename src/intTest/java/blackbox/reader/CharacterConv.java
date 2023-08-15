package blackbox.reader;

import java.util.Iterator;
import java.util.List;

final class CharacterConv {

    private static final String[] STANDARD = {" ", "\r", "\n"};
    private static final String[] CONV = {"␣", "␍", "␊"};
    private static final char FIELD_SEPARATOR = '↷';
    private static final char LINE_SEPARATOR = '⏎';
    private static final String EMPTY_STRING = "◯";
    private static final String EMPTY_LIST = "∅";

    private CharacterConv() {
    }

    public static String print(final List<List<String>> data) {
        if (data.isEmpty()) {
            return EMPTY_LIST;
        }

        final StringBuilder sb = new StringBuilder();
        for (Iterator<List<String>> iter = data.iterator(); iter.hasNext();) {
            final List<String> datum = iter.next();
            final Iterator<String> iterator = datum.iterator();
            while (iterator.hasNext()) {
                sb.append(print(iterator.next()));
                if (iterator.hasNext()) {
                    sb.append(FIELD_SEPARATOR);
                }
            }
            if (iter.hasNext()) {
                sb.append(LINE_SEPARATOR);
            }
        }
        return sb.toString();
    }

    public static String print(final String str) {
        return str.isEmpty() ? EMPTY_STRING : replaceEach(str, STANDARD, CONV);
    }

    @SuppressWarnings("PMD.UseVarargs")
    private static String replaceEach(final String text, final String[] searchList,
                                      final String[] replacementList) {
        String ret = text;
        for (int i = 0; i < searchList.length; i++) {
            ret = ret.replace(searchList[i], replacementList[i]);
        }
        return ret;
    }

    public static String parse(final String str) {
        return replaceEach(str, CONV, STANDARD);
    }

}
