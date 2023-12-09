package example;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.QuoteStrategy;

/**
 * Example for writing CSV data with different quote strategies.
 */
public class ExampleCsvWriterWithQuoteStrategy {

    public static void main(final String[] args) throws IOException {
        final PrintWriter pw = new PrintWriter(System.out, false, StandardCharsets.UTF_8);

        pw.println("Quote always");
        CsvWriter.builder()
            .quoteStrategy(QuoteStrategy.ALWAYS)
            .build(pw)
            .writeRecord("value1", "", null, "value,4");

        pw.println("Quote non-empty");
        CsvWriter.builder()
            .quoteStrategy(QuoteStrategy.NON_EMPTY)
            .build(pw)
            .writeRecord("value1", "", null, "value,4");

        pw.println("Quote empty");
        CsvWriter.builder()
            .quoteStrategy(QuoteStrategy.EMPTY)
            .build(pw)
            .writeRecord("value1", "", null, "value,4");

        pw.println("Quote custom");
        CsvWriter.builder()
            .quoteStrategy(customQuote())
            .build(pw)
            .writeRecord("value1", "", null, "value,4");

        pw.close();
    }

    // A quote strategy can be used to force quote fields that would otherwise not be quoted.
    private static QuoteStrategy customQuote() {
        return new QuoteStrategy() {
            @Override
            public boolean quoteNull(final int lineNo, final int fieldIdx) {
                // Default implementation returns false
                return false;
            }

            @Override
            public boolean quoteEmpty(final int lineNo, final int fieldIdx) {
                // Default implementation returns false
                return false;
            }

            @Override
            public boolean quoteNonEmpty(final int lineNo, final int fieldIdx, final String value) {
                // Default implementation returns false
                return "value1".equals(value);
            }
        };
    }

}
