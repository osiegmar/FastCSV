package de.siegmar.fastcsv;

final class CsvConstants {

    /**
     * Data to write CSV.
     */
    static final String[] RECORD = {
        "Simple field",
        "Example with separator ,",
        "Example with delimiter \"",
        "Example with\nnewline",
        "Example with , and \" and \nnewline",
    };

    /**
     * Data to read CSV.
     */
    static final String DATA = "Simple field,"
        + "\"Example with separator ,\","
        + "\"Example with delimiter \"\"\","
        + "\"Example with\nnewline\","
        + "\"Example with , and \"\" and \nnewline\""
        + "\n";

    private CsvConstants() {
    }

}
