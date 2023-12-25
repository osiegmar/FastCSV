# Migrating from 2.x to 3.x

- The minimum Java version is now 11 (compared to 8 in earlier versions)
- This also raised the required Android API level from version 26 (Android 8) to 33 (Android 13)
- Rows are now called Records (aligned to RFC 4180)
- Changes on the writer side:
  - `writeRow()` is now `writeRecord()` in `CsvWriter`
  - `QuoteStrategy` changed from an enum to an interface
  - `QuoteStretegy#REQUIRED` is removed as it is now the default (no quote strategy is needed)
- Changes on the reader side:
  - `CsvRow` is now `CsvRecord`
  - `skipEmptyRows` is now `skipEmptyLines`
  - `errorOnDifferentFieldCount` is now `ignoreDifferentFieldCount` (opposite meaning)
  - `getOriginalLineNumber` is now `getStartingLineNumber`
  - `MalformedCsvException` is now `CsvParseException` and is thrown instead of `IOException` for non-IO related errors
  - `NamedCsvReader` now needs an `CsvReader` as input (`NamedCsvReader.builder().from(csvReader)`) to reduce duplication of code
  - Limit the maximum field count per record to 16,384 to avoid OutOfMemoryErrors
  - Limit the maximum record size to four times the maximum field size to avoid OutOfMemoryErrors

See the https://github.com/osiegmar/FastCSV/tree/main/example/src/main/java/example for more examples.

If you're still on version 1, see https://github.com/osiegmar/FastCSV/blob/v2.2.2/UPGRADING.md
