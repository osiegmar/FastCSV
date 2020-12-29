# FastCSV

[![build](https://github.com/osiegmar/FastCSV/workflows/build/badge.svg?branch=version2-rewrite)](https://github.com/osiegmar/FastCSV/actions?query=branch%3Aversion2-rewrite)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/7270301676d6463bad9dd1fe23429942)](https://www.codacy.com/gh/osiegmar/FastCSV/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=osiegmar/FastCSV&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/osiegmar/FastCSV/branch/version2-rewrite/graph/badge.svg?token=WIWkv7HUyk)](https://app.codecov.io/gh/osiegmar/FastCSV/branch/version2-rewrite)
[![javadoc](https://javadoc.io/badge2/de.siegmar/fastcsv/javadoc.svg)](https://javadoc.io/doc/de.siegmar/fastcsv)
[![Maven Central](https://img.shields.io/maven-central/v/de.siegmar/fastcsv.svg)](https://search.maven.org/search?q=g:%22de.siegmar%22%20AND%20a:%22fastcsv%22)

FastCSV is an ultra-fast and dependency-free [RFC 4180](https://tools.ietf.org/html/rfc4180) compliant CSV
library for Java.

Actively developed and maintained since 2015 its primary intended use cases are:
- big data applications to read and write data on a massive scale
- small data applications with the need for a lightweight library

## Benchmark

Benchmark from the
[Java CSV library benchmark suite](https://github.com/osiegmar/JavaCsvBenchmarkSuite) project:

![Benchmark](benchmark.png "Benchmark")

## Features

### API

- Ultra fast
- Small footprint
- Zero runtime dependencies
- Null-free

### CSV specific

- RFC 4180 compliant – including:
  - Newline and field separator characters in fields
  - Quote escaping
- Configurable field separator
- Support for line endings CRLF (Windows), CR (old Mac OS) and LF (Unix)
- Unicode support

### Reader specific

- Support reading of some non-compliant (real world) data
- Preserving line break character(s) within fields
- Preserving the original line number (even with skipped and multi line records) –
  helpful for error messages
- Auto detection of line delimiters (can also be mixed)
- Configurable data validation
- Support for (optional) header lines (get field based on column name)
- Support for skipping empty rows
- Support for commented lines (skipping & reading) and configurable comment character

### Writer specific

- Support for multiple quote strategies to differentiate between empty and null

## Requirements

- Java 8

> :bulb: Android is not Java and is not officially supported.
> Although some basic checks are included in the continuous integration pipeline in order to
> verify that the library *should* work with Android 8.0 (API level 26).

## CsvReader Examples

Iterative reading of some CSV data from a string

```java
CsvReader.builder().build("foo1,bar1\r\nfoo2,bar2")
    .forEach(System.out::println);
```

Iterative reading of some CSV data with a header

```java
NamedCsvReader.builder().build("header 1,header 2\nfield 1,field 2")
    .forEach(row -> row.getField("header 2"));
```

Iterative reading of a CSV file

```java
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
[CsvReaderExample.java](src/example/java/example/CsvReaderExample.java)

## CsvWriter Examples

Iterative writing of some data to a writer

```java
CsvWriter.builder().build(new PrintWriter(System.out, true))
    .writeRow("header1", "header2")
    .writeRow("value1", "value2");
```

Iterative writing of a CSV file

```java
try (CsvWriter csv = CsvWriter.builder().build(path, charset)) {
    csv
        .writeRow("header1", "header2")
        .writeRow("value1", "value2");
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
[CsvWriterExample.java](src/example/java/example/CsvWriterExample.java)

## Upgrading from version 1.x

Please see [UPGRADING.md](UPGRADING.md) for an overview of the main functionality of 1.x
and how to upgrade them to version 2.
