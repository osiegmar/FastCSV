package de.siegmar.fastcsv;

final class Constants {

    /**
     * Data to write CSV.
     */
    static final String[] ROW = {
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

    private Constants() {
    }

}
