package de.siegmar.fastcsv.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UtilTest {

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x09, 0x0a, 0x0d, 0x1b, 0x1f, 0x7f, 0x85, 0x9f, 0xad, 0x0378, 0x2028, 0x2029, 0x202e,
        0xd83d, 0xde00, 0xe000, 0xfeff, 0xffff})
    void escapesCharacter(final int ch) {
        assertThat(Util.escapeControlCharacters("a" + (char) ch + "b"))
            .isEqualTo("a\\u%04xb".formatted(ch));
    }

    @ParameterizedTest
    @ValueSource(ints = {0x20, 0x41, 0x30, 0x2e, 0x7e, 0xa0, 0xe4, 0x0301, 0x2027, 0x3000, 0x1f600})
    void keepsOtherCharacter(final int codePoint) {
        final String str = Character.toString(codePoint);
        assertThat(Util.escapeControlCharacters(str))
            .isEqualTo(str);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\\n"})
    void keepsString(final String str) {
        assertThat(Util.escapeControlCharacters(str))
            .isEqualTo(str);
    }

    @Test
    void escapesSupplementaryFormatCharacter() {
        assertThat(Util.escapeControlCharacters("a" + Character.toString(0xe0001) + "b"))
            .isEqualTo("a\\udb40\\udc01b");
    }

    @Test
    void escapesSupplementaryPrivateUseCharacter() {
        assertThat(Util.escapeControlCharacters("a" + Character.toString(0xf0000) + "b"))
            .isEqualTo("a\\udb80\\udc00b");
    }

    @Test
    void escapesUnpairedSurrogatesSeparately() {
        assertThat(Util.escapeControlCharacters(new String(new char[]{0xde00, 0xd83d})))
            .isEqualTo("\\ude00\\ud83d");
    }

    @SuppressWarnings("checkstyle:IllegalTokenText")
    @Test
    void escapesEveryOccurrence() {
        assertThat(Util.escapeControlCharacters("\r\na\n"))
            .isEqualTo("\\u000d\\u000aa\\u000a");
    }

}
