# FastCSV

[![Build Status](https://travis-ci.org/osiegmar/FastCSV.svg?branch=version2-rewrite)](https://travis-ci.org/osiegmar/FastCSV)
[![codecov](https://codecov.io/gh/osiegmar/FastCSV/branch/version2-rewrite/graph/badge.svg?token=WIWkv7HUyk)](https://app.codecov.io/gh/osiegmar/FastCSV/branch/version2-rewrite)
[![javadoc](https://javadoc.io/badge2/de.siegmar/fastcsv/javadoc.svg)](https://javadoc.io/doc/de.siegmar/fastcsv)
[![Maven Central](https://img.shields.io/maven-central/v/de.siegmar/fastcsv.svg)](https://search.maven.org/search?q=g:%22de.siegmar%22%20AND%20a:%22fastcsv%22)

FastCSV is an ultra fast and dependency-free [RFC 4180](https://tools.ietf.org/html/rfc4180) compliant CSV
library for Java.

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
- Configurable quoting
- Support for line endings CRLF (Windows), CR (old Mac OS) and LF (Unix)
- Support for (optional) header lines (get field based on column name)
- Support for multiple line values (using the text delimiter)
- Support for field separator character in value (using the text delimiter)
- Support for reading and writing in an iterative or all at once way
- Support for skipping empty rows and preserving the original line number (useful for error messages)
- Support for commented lines
- Support for multiple quote strategies to differentiate between empty and null

## Requirements

- Java 8

## CsvReader Examples

Iterative reading of some CSV data from a string

```java
CsvReader.builder().build(new StringReader("foo1,bar1\r\nfoo2,bar2"))
    .forEach(System.out::println);
```

Iterative reading of a CSV file

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
    .quoteCharacter('"')
    .commentStrategy(CommentStrategy.SKIP)
    .commentCharacter('#')
    .skipEmptyRows(true)
    .errorOnDifferentFieldCount(false);
```

For more example see
[CsvReaderExampleTest.java](src/test/java/de/siegmar/fastcsv/reader/CsvReaderExampleTest.java)

## CsvWriter Examples

Iterative writing of some data to a writer

```java
CsvWriter.builder().build(new PrintWriter(System.out, true))
    .writeLine("header1", "header2")
    .writeLine("value1", "value2");
```

Iterative writing of a CSV file

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
    .quoteCharacter('"')
    .quoteStrategy(QuoteStrategy.REQUIRED)
    .lineDelimiter(LineDelimiter.LF);
```

For more example see
[CsvWriterExampleTest.java](src/test/java/de/siegmar/fastcsv/writer/CsvWriterExampleTest.java)

## Contribution

- Fork
- Code
- Add test(s)
- Commit
- Send me a pull request
