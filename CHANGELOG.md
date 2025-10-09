# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

<!-- JRELEASER_CHANGELOG_APPEND - Do not remove or modify this section -->
## [v4.1.0] - 2025-10-09
## Release v4.1.0

## 🚀 Features
- 960d8c8 ignore same comment character if comments are ignored

## [v4.0.0] - 2025-06-22

## 🚀 Features
- 18688cc add returnHeader to NamedCsvRecordHandler to allow early-access to the header #147, closes #147
- e006347 add `ofSingleCsvRecord` methods to `CsvReader` for convenience
- 60774d3 🚨 enforce unique headers by default
- bd9991f introduce relaxed parsing mode for CsvReader

## 🐛 Fixes
- 76dff29 don't call peek line predicate with empty string if reached EOF

## 🔄️ Changes
- a565316 add missing finals
- 57da808 optimize performance of relaxed parser
- 77b986c change skipLine method to void and handle EOF exceptions
- c815d3e simplify BOM handling in CsvReader #149, closes #149
- 52fe46d add class retention to allow incremental builds
- cef4a2b later buffer expansion
- 47cca27 refactor EOF handling when peeking lines
- bba1412 introduce Nullable annotation
- 98aeaab 🚨 return Optional for throwable in status listener
- 1130197 use errorprone
- ef14cb7 change exception type from IllegalArgumentException to IllegalStateException for relaxed mode validation
- b1b5f23 simplify quoted parsing logic in RelaxedCsvParser
- 08194a4 remove dead code
- ced8dd3 introduce LookaheadReader to improve the performance of RelaxedCsvParser
- 1bc5f26 🚨 convert FieldModifiers class to enum and move modify method to FieldModifier interface
- d1e5943 remove unnecessary 'this' keyword in variable assignments
- cb5a999 🚨 rename quoteNonEmpty method to quoteValue and made quoteStrategy non-nullable
- 5fbfe91 update method name for allowing duplicate header fields in NamedCsvRecordHandler
- 5980793 simplify materializeField logic and adjust return flow
- 056c865 🚨 CSV callback handling and record type logic
- 85bbfdf 🚨 Refactor field count handling in CsvReader
- 4107892 simplify validation error messages and add a new test
- dd00979 extract csv parser interface to allow multiple implementations
- 3ac07d1 🚨 strict handling of characters after closing quote, by default
- 9d8511b seal AbstractInternalCsvCallbackHandler for internal use (as documented before)
- a33384e use unnamed variables (_) in lambdas to simplify tests
- 2babea6 🚨 disable automatic buffer flushing for writer use
- 403c2f6 use skipNBytes for skipping a detected BOM
- 0d05add 🚨 removed deprecated code (Limits and SimpleFieldModifier)
- 9e620a3 corrected method name in error message
- 006380c 🚨 remove the RecordWrapper
- 430adef add @Serial annotation
- 724bc38 🚨 changed implementation of CsvIndex and CsvPage to Java records
- 3e11bfc use formatted Strings
- 01f5cb3 use switch expression

## 🧪 Tests
- 6e43efb add benchmark for relaxed parser
- 150b68e add missing tests

## 🧰 Tasks
- 3e3edcb update dependencies for test and build; refactor internal code to keep SpotBugs happy
- 0a0136c prepare (clean) upgrading.md for a new major release
- ba1a894 bump version to 4.0.0-SNAPSHOT

## 🛠  Build
- f990d39 update resolver plugin
- 03f4a49 bump version to 4.0.0
- f14dab0 updated Gradle
- 0d97d92 updated build/test dependencies
- 2cd4ac1 add SPDX-License-Identifier to MANIFEST.MF
- a3aafac update pitest version for Java 24 compatibility
- 541b39e include LICENSE file in META-INF directory for use in dependent libraries
- d14aef6 enable parameter names for tests with @ParameterizedTest
- 7584ab9 update PMD and remove unnecessary suppressions
- d8fadf0 🚨 require Java 17 / Android 34
- 280ab79 let JReleaser manage the CHANGELOG.md

## 📝 Documentation
- c248a38 document array wrapping usage
- f10e7ea update copyright year in LICENSE file
- e5fff5a align with Java 17

## [3.7.0] - 2025-05-11
### Added
- `FieldModifiers.modify(Function<String, String>)` to simply modify fields via functional interface

### Deprecated
- `SimpleFieldModifier` interface
- `FieldModifiers.lower` and `FieldModifiers.upper` methods

## [3.6.0] - 2025-03-04
### Added
- Configuration of maximum fields, maximum field size, and maximum record size via record handler
- Fluent configuration for `CsvRecordHandler`, `NamedCsvRecordHandler`, and `StringArrayHandler`
- `maxBufferSize(int)` in `CsvReaderBuilder` and `IndexedCsvReaderBuilder` to alter the maximum buffer size of the parser

### Changed
- Use ReentrantLock in IndexedCsvReader instead of synchronized to prevent pinning of virtual threads
- More precise error messages when exceeding the maximum field or record size
- More precise error when parsing error occurs within `IndexedCsvReader`
- Apply length constraints (maximum field size and maximum record size) **after** applying field modifiers

### Deprecated
- Setting the maximum field size (and maximum buffer size) via system property `fastcsv.max.field.size`
- Setting the maximum field count per record via system property `fastcsv.max.field.count`
- Constructor initialization of `CsvRecordHandler`, `NamedCsvRecordHandler`, and `StringArrayHandler`

## [3.5.0] - 2025-02-22
### Added
- Support InputStream as an input source for CsvReader including BOM detection [\#130](https://github.com/osiegmar/FastCSV/issues/130)
- Support OutputStream as an output target for CsvWriter

## [3.4.0] - 2024-11-04
### Added
- Add `skipLines(int)` and `skipLines(Predicate<String>, int)` to `CsvReader` to skip lines before the actual CSV data starts

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

[Unreleased]: https://github.com/osiegmar/FastCSV/compare/v4.0.0...HEAD
[v4.0.0]: https://github.com/osiegmar/FastCSV/compare/v3.7.0...v4.0.0
[3.7.0]: https://github.com/osiegmar/FastCSV/compare/v3.6.0...v3.7.0
[3.6.0]: https://github.com/osiegmar/FastCSV/compare/v3.5.0...v3.6.0
[3.5.0]: https://github.com/osiegmar/FastCSV/compare/v3.4.0...v3.5.0
[3.4.0]: https://github.com/osiegmar/FastCSV/compare/v3.3.1...v3.4.0
[3.3.1]: https://github.com/osiegmar/FastCSV/compare/v3.3.0...v3.3.1
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
