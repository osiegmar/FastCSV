<p align="center">
  <img src="fastcsv.svg" width="400" height="50" alt="FastCSV">
</p>

<p align="center">
  FastCSV is a lightning-fast, dependency-free CSV library for Java that conforms to RFC standards.
</p>

<p align="center">
  <a href="https://github.com/osiegmar/FastCSV/actions/workflows/build.yml"><img src="https://github.com/osiegmar/FastCSV/actions/workflows/build.yml/badge.svg?branch=main" alt="build"></a>
  <a href="https://app.codacy.com/gh/osiegmar/FastCSV/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade"><img src="https://app.codacy.com/project/badge/Grade/7270301676d6463bad9dd1fe23429942" alt="Codacy Badge"></a>
  <a href="https://codecov.io/gh/osiegmar/FastCSV"><img src="https://codecov.io/gh/osiegmar/FastCSV/branch/main/graph/badge.svg?token=WIWkv7HUyk" alt="codecov"></a>
  <a href="https://javadoc.io/doc/de.siegmar/fastcsv"><img src="https://javadoc.io/badge2/de.siegmar/fastcsv/javadoc.svg" alt="javadoc"></a>
  <a href="https://central.sonatype.com/artifact/de.siegmar/fastcsv"><img src="https://img.shields.io/maven-central/v/de.siegmar/fastcsv" alt="Maven Central"></a>
</p>

------

The primary use cases of FastCSV include:

- In *big data* applications: efficiently reading and writing data on a massive scale.
- In *small data* applications: serving as a lightweight library without additional dependencies.

## Benchmark & Compatibility

A selected benchmark from the
[Java CSV library benchmark suite](https://github.com/osiegmar/JavaCsvBenchmarkSuite) project:

![Benchmark](benchmark.png "Benchmark")

While maintaining high performance, FastCSV serves as a strict RFC 4180 CSV writer while
also exhibiting the ability to read somewhat garbled CSV data.
See [JavaCsvComparison](https://github.com/osiegmar/JavaCsvComparison) for details.

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
- Preserving the starting line number (even with skipped and multi line records) –
  helpful for error messages
- Auto-detection of line delimiters (can also be mixed)
- Configurable data validation
- Support for (optional) header records (get field based on field name)
- Support for skipping empty records
- Support for commented lines (skipping & reading) and configurable comment character
- BOM support (UTF-8, UTF-16 and UTF-32 with little- or big-endian)

### Writer specific

- Support for multiple quote strategies to differentiate between empty and null
- Support for writing comments with proper quotation if needed

## Requirements

- for 3.x version: Java ⩾ 11 (Android 13 / API level 33)
- for 2.x version: Java ⩾ 8 (Android 8 / API level 26)

> :bulb: Android is not Java and is not officially supported.
> Nevertheless, some basic checks are included in the continuous integration pipeline to
> verify that the library *should* work with Android.

## CsvReader examples

Iterative reading of some CSV data from a string

```java
CsvReader.builder().build("foo1,bar1\nfoo2,bar2")
    .forEach(System.out::println);
```

Iterative reading of a CSV file

```java
try (CsvReader csv = CsvReader.builder().build(file)) {
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
    .skipEmptyLines(true)
    .ignoreDifferentFieldCount(false)
    .detectBomHeader(false)
    .fieldModifier(FieldModifier.TRIM);
```

## NamedCsvReader examples

Iterative reading of some CSV data with a header

```java
CsvReader csvReader = CsvReader.builder().build("header 1,header 2\nfield 1,field 2");
NamedCsvReader.from(csvReader)
    .forEach(csvRecord -> System.out.println(csvRecord.getField("header 2")));
```

Iterative reading of some CSV data with a custom header

```java
List<String> header = List.of("header 1", "header 2");
CsvReader csvReader = CsvReader.builder().build("field 1,field 2");
NamedCsvReader.from(csvReader)
    .forEach(csvRecord -> System.out.println(csvRecord.getField("header 2")));
```

## IndexedCsvReader examples

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

## CsvWriter examples

Iterative writing of some data to a writer

```java
var sw = new StringWriter();
CsvWriter.builder().build(sw)
    .writeRecord("header1", "header2")
    .writeRecord("value1", "value2");

System.out.println(sw);
```

Iterative writing of a CSV file

```java
try (CsvWriter csv = CsvWriter.builder().build(file)) {
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
    .quoteStrategy(QuoteStrategy.ALWAYS)
    .commentCharacter('#')
    .lineDelimiter(LineDelimiter.LF);
```

## More examples

For more examples see [example/src/main/java/example/](example/src/main/java/example/).

## Upgrading from an older version

Please see [UPGRADING.md](UPGRADING.md) for a list of breaking changes in version 3 and how to upgrade.

---

## Sponsoring and partnerships

![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit was used to optimize the performance and footprint of FastCSV.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.
