package blackbox.reader;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

final class CharacterConv {

    private static final String[] STANDARD = {" ", "\r", "\n" };
    private static final String[] CONV = {"␣", "␍", "␊" };
    private static final char FIELD_SEPARATOR = '↷';
    private static final char LINE_SEPARATOR = '⏎';
    private static final String EMPTY_STRING = "◯";
    private static final String EMPTY_LIST = "∅";

    private CharacterConv() {
    }

    public static String print(final List<String[]> data) {
        if (data.isEmpty()) {
            return EMPTY_LIST;
        }

        final StringBuilder sb = new StringBuilder();
        for (Iterator<String[]> iter = data.iterator(); iter.hasNext();) {
            final String[] datum = iter.next();
            final Iterator<String> iterator = Arrays.stream(datum).iterator();
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
        return str.isEmpty() ? EMPTY_STRING : StringUtils.replaceEach(str, STANDARD, CONV);
    }

    public static String parse(final String str) {
        return StringUtils.replaceEach(str, CONV, STANDARD);
    }

}
