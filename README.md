# FastCSV

[![Build Status](https://travis-ci.org/osiegmar/FastCSV.svg?branch=master)](https://travis-ci.org/osiegmar/FastCSV)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.siegmar/fastcsv/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.siegmar/fastcsv)

FastCSV is a ultra fast and simple [RFC 4180](https://tools.ietf.org/html/rfc4180) compliant CSV
library for Java, licensed under the Apache License, Version 2.0.

## Benchmark

Benchmark from the
[Java CSV library benchmark suite](https://github.com/osiegmar/JavaCsvBenchmarkSuite) project:

![Benchmark](benchmark.png "Benchmark")

## Features

- RFC 4180 compliant CSV reader and writer
- Ultra fast
- Small footprint
- Zero runtime dependencies
- Configurable field separator
- Configurable text delimiter
- Support for line endings CRLF (Windows), CR (old Mac OS) and LF (Unix)
- Support for (optional) header lines (get field based on column name)
- Support for multiple line values (using the text delimiter)
- Support for field separator character in value (using the text delimiter)
- Support for reading and writing in an iterative or all at once way
- Support for skipping empty rows and preserving the original line number (useful for error messages)

## Requirements

- Java 8

## CsvReader Examples

Iterative reading of a CSV file (RFC standard format, UTF-8 encoded)

```java
Path path = Paths.get("foo.csv");
Charset charset = StandardCharsets.UTF_8;

try (CsvReader csv = CsvReader.builder().build(path, charset)) {
    csv.forEach(System.out::println);
}
```

Custom settings

```java
CsvReader.builder()
    .fieldSeparator(';')
    .textDelimiter('"')
    .skipEmptyRows(true)
    .errorOnDifferentFieldCount(false);
```

For more example see
[CsvReaderExampleTest.java](src/test/java/de/siegmar/fastcsv/reader/CsvReaderExampleTest.java)

## CsvWriter Examples

Iterative writing of a CSV file (RFC standard format, UTF-8 encoded)

```java
Path path = Files.createTempFile("fastcsv", ".csv");
Charset charset = StandardCharsets.UTF_8;

try (CsvWriter csv = CsvWriter.builder().build(path, charset)) {
    csv
        .writeLine("header1", "header2")
        .writeLine("value1", "value2");
}
```

Custom settings

```java
CsvWriter.builder()
    .fieldSeparator(',')
    .textDelimiter('"')
    .textDelimitStrategy(TextDelimitStrategy.REQUIRED)
    .lineDelimiter("\n");
```

For more example see
[CsvWriterExampleTest.java](src/test/java/de/siegmar/fastcsv/writer/CsvWriterExampleTest.java)


## Contribution

- Fork
- Code
- Add test(s)
- Commit
- Send me a pull request

## Copyright

Copyright 2020 Oliver Siegmar

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
