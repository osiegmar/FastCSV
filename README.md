<p align="center">
  <img src="fastcsv.svg" width="400" height="50" alt="FastCSV">
</p>

<p align="center">
  FastCSV is an ultra-fast, dependency-free and RFC-compliant CSV library for Java.
</p>

<p align="center">
  <a href="https://github.com/osiegmar/FastCSV/actions/workflows/build.yml"><img src="https://github.com/osiegmar/FastCSV/actions/workflows/build.yml/badge.svg?branch=master" alt="build"></a>
  <a href="https://app.codacy.com/gh/osiegmar/FastCSV/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade"><img src="https://app.codacy.com/project/badge/Grade/7270301676d6463bad9dd1fe23429942" alt="Codacy Badge"></a>
  <a href="https://codecov.io/gh/osiegmar/FastCSV"><img src="https://codecov.io/gh/osiegmar/FastCSV/branch/master/graph/badge.svg?token=WIWkv7HUyk" alt="codecov"></a>
  <a href="https://javadoc.io/doc/de.siegmar/fastcsv"><img src="https://javadoc.io/badge2/de.siegmar/fastcsv/javadoc.svg" alt="javadoc"></a>
  <a href="https://central.sonatype.com/artifact/de.siegmar/fastcsv"><img src="https://img.shields.io/maven-central/v/de.siegmar/fastcsv" alt="Maven Central"></a>
</p>

------

The primary use cases of FastCSV are:
- in big data applications: read and write data on a massive scale
- in small data applications: a lightweight library without any further dependencies

## Benchmark & Compatibility

A selected benchmark from the
[Java CSV library benchmark suite](https://github.com/osiegmar/JavaCsvBenchmarkSuite) project:

![Benchmark](benchmark.png "Benchmark")

While maintaining high performance, FastCSV is a strict RFC 4180 CSV writer but also able
to read garbled CSV data (to some degree). See [JavaCsvComparison](https://github.com/osiegmar/JavaCsvComparison) for details.

## Features

### Library specific

- Ultra fast
- Small footprint
- Zero runtime dependencies
- Null-free
- Works with GraalVM Native Image
- OSGi capable

### CSV specific

- Compliant to [RFC 4180](https://tools.ietf.org/html/rfc4180) – including:
  - Newline and field separator characters in fields
  - Quote escaping
- Configurable field separator
- Support for line endings CRLF (Windows), CR (old macOS) and LF (Unix)
- Unicode support

### Reader specific

- Support reading of some non-compliant (real world) data
- Preserving line break character(s) within fields
- Preserving the original line number (even with skipped and multi line records) –
  helpful for error messages
- Auto-detection of line delimiters (can also be mixed)
- Configurable data validation
- Support for (optional) header lines (get field based on column name)
- Support for skipping empty records
- Support for commented lines (skipping & reading) and configurable comment character

### Writer specific

- Support for multiple quote strategies to differentiate between empty and null
- Support for writing comments with proper quotation if needed

## Requirements

- for 3.x version: Java 11 (Android 13 / API level 33)
- for 2.x version: Java 8 (Android 8 / API level 26)

> :bulb: Android is not Java and is not officially supported.
> Although some basic checks are included in the continuous integration pipeline in order to
> verify that the library *should* work with Android.

## CsvReader Examples

Iterative reading of some CSV data from a string

```java
CsvReader.builder().build("foo1,bar1\r\nfoo2,bar2")
    .forEach(System.out::println);
```

Iterative reading of a CSV file

```java
try (CsvReader csv = CsvReader.builder().build(path)) {
    csv.forEach(System.out::println);
}
```

### Custom settings

```java
CsvReader.builder()
    .fieldSeparator(';')
    .quoteCharacter('"')
    .commentStrategy(CommentStrategy.SKIP)
    .commentCharacter('#')
    .skipEmptyRecords(true)
    .errorOnDifferentFieldCount(false);
```

For more examples see [CsvReaderExample.java](example/src/main/java/example/CsvReaderExample.java)

## NamedCsvReader Examples

Iterative reading of some CSV data with a header

```java
NamedCsvReader.builder().build("header 1,header 2\nfield 1,field 2")
    .forEach(csvRecord -> System.out.println(csvRecord.getField("header 2")));
```

For more examples see [NamedCsvReaderExample.java](example/src/main/java/example/NamedCsvReaderExample.java)

## IndexedCsvReader Examples

Indexed reading of a CSV file

```java
try (IndexedCsvReader csv = IndexedCsvReader.builder().build(file)) {
    CsvIndex index = csv.index();

    System.out.println("Items of last page:");
    int lastPage = index.pageCount() - 1;
    List<CsvRecord> csvRecords = csv.readPage(lastPage);
    csvRecords.forEach(System.out::println);
}
```

For more examples see [IndexedCsvReaderExample.java](example/src/main/java/example/IndexedCsvReaderExample.java)

## CsvWriter Examples

Iterative writing of some data to a writer

```java
CsvWriter.builder().build(new PrintWriter(System.out, true))
    .writeRecord("header1", "header2")
    .writeRecord("value1", "value2");
```

Iterative writing of a CSV file

```java
try (CsvWriter csv = CsvWriter.builder().build(path)) {
    csv
        .writeRecord("header1", "header2")
        .writeRecord("value1", "value2");
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

For more examples see
[CsvWriterExample.java](example/src/main/java/example/CsvWriterExample.java).

## Upgrading from an older version

Please see [UPGRADING.md](UPGRADING.md) for a list of breaking changes in version 3 and how to upgrade.

---

## Sponsoring and partnerships

![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit was used to optimize the performance and footprint of FastCSV.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.
