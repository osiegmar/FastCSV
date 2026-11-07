package de.siegmar.fastcsv.reader;

/// Provides some common [FieldModifier] implementations.
///
/// Example usage:
/// ```
/// FieldModifier modifier = FieldModifiers.TRIM
///     .andThen(FieldModifier.modify(field -> field.toUpperCase(Locale.ENGLISH)));
/// CsvRecordHandler handler = CsvRecordHandler.of(c -> c.fieldModifier(modifier));
/// CsvRecord csvRecord = CsvReader.builder().ofSingleCsvRecord(handler, "  foo   ,   bar");
/// // field contents: "FOO" and "BAR"
/// ```
public enum FieldModifiers implements FieldModifier {

    /// Modifier that does not modify anything.
    NOP,

    /// Modifier that trims the field content with [String#trim()]: it removes leading and trailing whitespace
    /// (any character from `U+0000` to `U+0020`), also in quoted fields.
    /// Comments are not modified.
    TRIM {
        @Override
        public String modify(final long startingLineNumber, final int fieldIdx,
                             final boolean quoted, final String field) {
            return field.trim();
        }
    },

    /// Modifier that modifies the field content with [String#strip()]: it removes leading and trailing white space
    /// as defined by [Character#isWhitespace(int)], also in quoted fields. This is a different set of characters
    /// than `U+0000` to `U+0020`.
    /// Comments are not modified.
    STRIP {
        @Override
        public String modify(final long startingLineNumber, final int fieldIdx,
                             final boolean quoted, final String field) {
            return field.strip();
        }
    }

}
