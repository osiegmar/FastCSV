# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- Nothing yet

## [3.3.1] - 2024-09-23
### Fixed
- Fixed a bug in CsvReader where lines were mistakenly treated as empty and skipped when skipEmptyLines was set (default). These affected lines made up solely of field separators, solely empty quoted fields, or fields rendered empty after applying optional field modifiers.

## [3.3.0] - 2024-09-19
### Added
- Implement `Flushable` interface for `CsvWriter` to allow flushing the underlying writer
- Implement `autoFlush` option for `CsvWriter` to automatically flush the writer after writing a record
- Implement `toConsole` method for `CsvWriter` to write records to the console

## [3.2.0] - 2024-06-15
### Added
- Add `writeRecord()` to `CsvWriter` to allow writing records field by field
- Allow overwriting the limits of 16K fields per record and 16M characters per field (#104); Thanks to [@Obolrom](https://github.com/Obolrom)!

## [3.1.0] - 2024-03-09
### Added
- Add acceptCharsAfterQuotes() to CsvReaderBuilder and IndexedReaderBuilder to allow even stricter parsing

### Changed
- Improved reader performance

## [3.0.0] - 2024-01-10
### Added
- `IndexedCsvReader` for random access to CSV files
- `FieldModifier` for modifying fields while reading CSV files
- Allow custom quote strategies for CsvWriter
- `CsvCallbackHandler` for more flexible usage of CsvReader
- Support for optional BOM header when reading CSV files
- Method `NamedCsvRecord.findField` for optional field access
- Allow READ comment strategy for CSV data with a header
- Metadata for OSGi capability

### Changed
- Updated from Java 8 to Java 11
- Updated naming (rows/lines -> records, columns -> fields, differentiate between lines and records)
- `NamedCsvReader` replaced by `CsvReader.builder().ofNamedCsvRecord()`
- `build` methods in `CsvReaderBuilder` with callback handlers and `ofCsvRecord` / `ofNamedCsvRecord` as convenience methods
- Rename `errorOnDifferentFieldCount()` to `ignoreDifferentFieldCount()`
- `QuoteStrategy` is now an interface – defaults are provided by `QuoteStrategies`
- Throw `CsvParseException` instead of `IOException` when maximum field size is exceeded
- `NamedCsvRecord` extends `CsvRecord` and provides more access methods
- Raised the maximum field size to 16 MiB to match SUPER data type capabilities of Amazon Redshift
- Limit the maximum field count per record to 16,384 to prevent OutOfMemoryErrors
- Limit the maximum record size to 64 MiB to prevent OutOfMemoryErrors
- Several performance improvements
- Improved documentation and error messages

### Removed
- Removed `isEmpty()` in `CsvRecord` as it was formerly only used for skipping empty records

### Fixed
- Do not throw an exception when reading comments while enabling different field count checking

## [2.2.2] - 2023-05-13
### Added
- New quote strategy that adds quotes only for non-empty fields [\#80](https://github.com/osiegmar/FastCSV/pull/80)

## [2.2.1] - 2022-11-09
### Fixed
- Fixed a problem when refilling the input buffer while parsing nonconforming data (quote character within unquoted field) [\#67](https://github.com/osiegmar/FastCSV/issues/67)

## [2.2.0] - 2022-06-20
### Added
- Improved CsvReader performance for String input [\#63](https://github.com/osiegmar/FastCSV/issues/63)
- Added configurable buffer size for CsvWriter [\#63](https://github.com/osiegmar/FastCSV/issues/63)

### Removed
- Erroneous random access file feature [\#59](https://github.com/osiegmar/FastCSV/issues/59)

## [2.1.0] - 2021-10-17
### Added
- Builder methods for standard encoding (UTF-8)
- Comment support for writer
- toString() method to CsvWriter and CsvWriterBuilder
- Support for random access file operations

### Changed
- Improved error message when buffer exceeds (because of invalid CSV data) [\#52](https://github.com/osiegmar/FastCSV/issues/52)
- Defined 'de.siegmar.fastcsv' as the Automatic-Module-Name (JPMS module name)

## [2.0.0] - 2021-01-01
### Added
- Support for commented lines [\#31](https://github.com/osiegmar/FastCSV/issues/31)
- Support for multiple quoting strategies [\#39](https://github.com/osiegmar/FastCSV/issues/39)

### Changed
- Completely re-engineered the API for better usability
- Improved performance
- Make use of Java 8 features (like Streams and Optionals)
- Replaced TestNG with JUnit 5
- Changed license from Apache 2.0 to MIT

### Removed
- CsvContainer concept – use `Stream.collect()` as a replacement
- `java.io.File` API – use `java.nio.file.Path` instead

## [1.0.4] - 2020-11-29
### Fixed
- Fix null returning CsvContainer when only a header is present [\#38](https://github.com/osiegmar/FastCSV/issues/38)

### Changed
- Remove unnecessary temporary objects in CsvAppender [\#8](https://github.com/osiegmar/FastCSV/issues/8)

## [1.0.3] - 2018-10-06
### Fixed
- Fix dropping empty quoted fields [\#19](https://github.com/osiegmar/FastCSV/issues/19)

## [1.0.2] - 2018-02-03
### Fixed
- Fix reading of non RFC 4180 compliant CSV data [\#2](https://github.com/osiegmar/FastCSV/issues/2)

### Changed
- Refactored csv parser code

## [1.0.1] - 2016-03-20
### Changed
- Replaced Maven with Gradle (and cleaned up / reformatted code for checkstyle update)

## 1.0.0 - 2015-04-03

- Initial release

[Unreleased]: https://github.com/osiegmar/FastCSV/compare/v3.3.1...main
[3.3.0]: https://github.com/osiegmar/FastCSV/compare/v3.3.0...v3.3.1
[3.3.0]: https://github.com/osiegmar/FastCSV/compare/v3.2.0...v3.3.0
[3.2.0]: https://github.com/osiegmar/FastCSV/compare/v3.1.0...v3.2.0
[3.1.0]: https://github.com/osiegmar/FastCSV/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/osiegmar/FastCSV/compare/v2.2.2...v3.0.0
[2.2.2]: https://github.com/osiegmar/FastCSV/compare/v2.2.1...v2.2.2
[2.2.1]: https://github.com/osiegmar/FastCSV/compare/v2.2.0...v2.2.1
[2.2.0]: https://github.com/osiegmar/FastCSV/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/osiegmar/FastCSV/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/osiegmar/FastCSV/compare/v1.0.4...v2.0.0
[1.0.4]: https://github.com/osiegmar/FastCSV/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/osiegmar/FastCSV/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/osiegmar/FastCSV/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/osiegmar/FastCSV/compare/v1.0.0...v1.0.1
