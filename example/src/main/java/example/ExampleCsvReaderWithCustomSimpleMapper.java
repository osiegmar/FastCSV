package example;

import de.siegmar.fastcsv.reader.CsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvReader;

/**
 * Example for simple mapper callback handler.
 */
public class ExampleCsvReaderWithCustomSimpleMapper {

    private static final String DATA = "john smith,56\njane doe,38";

    public static void main(final String[] args) {
        final CsvCallbackHandler<Person> mapper = CsvCallbackHandler.forSimpleMapper(
            fields -> new Person(fields[0], Integer.parseInt(fields[1]))
        );

        System.out.println("Mapping data with simple mapper callback handler:");
        for (final Person person : CsvReader.builder().build(mapper, DATA)) {
            System.out.println(person);
        }
    }

    private record Person(String name, int age) {
    }

}
