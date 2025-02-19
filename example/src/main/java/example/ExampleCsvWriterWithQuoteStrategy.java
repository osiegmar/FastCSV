package example;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.QuoteStrategies;
import de.siegmar.fastcsv.writer.QuoteStrategy;

/// Example for writing CSV data with different quote strategies.
public class ExampleCsvWriterWithQuoteStrategy {

    public static void main(final String[] args) {
        System.out.println("Quote always");
        CsvWriter.builder()
            .quoteStrategy(QuoteStrategies.ALWAYS)
            .toConsole()
            .writeRecord("value1", "", null, "value,4");

        System.out.println("Quote non-empty");
        CsvWriter.builder()
            .quoteStrategy(QuoteStrategies.NON_EMPTY)
            .toConsole()
            .writeRecord("value1", "", null, "value,4");

        System.out.println("Quote empty");
        CsvWriter.builder()
            .quoteStrategy(QuoteStrategies.EMPTY)
            .toConsole()
            .writeRecord("value1", "", null, "value,4");

        System.out.println("Quote custom");
        CsvWriter.builder()
            .quoteStrategy(customQuote())
            .toConsole()
            .writeRecord("value1", "", null, "value,4");
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
