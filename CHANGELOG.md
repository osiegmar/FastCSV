# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - 2021-??-??
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

[Unreleased]: https://github.com/osiegmar/FastCSV/compare/v2.0.0...develop
[2.0.0]: https://github.com/osiegmar/FastCSV/compare/v1.0.4...v2.0.0
[1.0.4]: https://github.com/osiegmar/FastCSV/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/osiegmar/FastCSV/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/osiegmar/FastCSV/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/osiegmar/FastCSV/compare/v1.0.0...v1.0.1
