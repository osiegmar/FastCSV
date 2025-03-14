---
title: Upgrading
description: Guide on how to upgrade from earlier versions of FastCSV to the latest version.
sidebar:
  order: 3
---

This document describes the **breaking** changes when upgrading from earlier versions of FastCSV to 3.x.
For a full list of changes, including new features, see the [changelog](https://github.com/osiegmar/FastCSV/blob/main/CHANGELOG.md).

## Upgrading from 2.x

This section describes the **breaking** changes when migrating from FastCSV 2.x to 3.x.

### Requirement changes

- The minimum Java version is now 11 (compared to 8 in earlier versions)
- This also raised the required Android API level from version 26 (Android 8) to 33 (Android 13)

### New limitations

- The maximum number of fields per record is now limited to 16,384 to avoid `OutOfMemoryErrors` caused
  by excessive field counts.
- The maximum record size is now limited to (64 MiB) to prevent `OutOfMemoryErrors`.

### Naming changes

#### Rows are now called Records (aligned to RFC 4180)

**Reading CSV records:**

```java
try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(file)) {
    csv.forEach(System.out::println);
}
```

**Write CSV records:**

```java
try (CsvWriter csv = CsvWriter.builder().build(file)) {
    csv
        .writeRecord("header1", "header2")
        .writeRecord("value1", "value2");
}
```

#### Method names

- In `CsvReaderBuilder`:
    - `skipEmptyRows` is now `skipEmptyLines`
    - `errorOnDifferentFieldCount` is now `ignoreDifferentFieldCount` (opposite meaning!)
    - `build` methods with callback handlers and `ofCsvRecord` / `ofNamedCsvRecord` as convenience methods
- In `CsvRecord` (former `CsvRow`):
    - `getOriginalLineNumber` is now `getStartingLineNumber`

### NamedCsvReader removed/replaced

A distinct `NamedCsvReader` is no longer needed as the `CsvReader` now supports callbacks for header and record
processing.

```java
CsvReader.builder().ofNamedCsvRecord("header 1,header 2\nfield 1,field 2")
    .forEach(rec -> System.out.println(rec.getField("header2")));
```

or with a custom header:

```java
NamedCsvRecordHandler callbackHandler = NamedCsvRecordHandler.builder()
    .header("header1", "header2")
    .build();

CsvReader.builder().build(callbackHandler, "field 1,field 2")
    .forEach(rec -> System.out.println(rec.getField("header2")));
```

### Extended/Refactored quote strategies

- `QuoteStrategy` changed from an enum to an interface (see `QuoteStrategies` for the default implementations)
- The `REQUIRED` quote strategy is removed as it is the default (no quote strategy is needed)

**Example to enable always quoting:**

```java
CsvWriter.builder()
    .quoteStrategy(QuoteStrategies.ALWAYS);
```

### Exception changes

`MalformedCsvException` is now `CsvParseException` and is thrown instead of `IOException` for non-IO related errors.

## Upgrading from 1.x

This section describes the **breaking** changes when migrating from FastCSV 1.x to 3.x.

### Reader

#### Configuring the reader

Old way:
```java
CsvReader csvReader = new CsvReader();
csvReader.setFieldSeparator(',');
csvReader.setTextDelimiter('"');
csvReader.setSkipEmptyRows(true);
csvReader.setErrorOnDifferentFieldCount(false);
```

New way:
```java
CsvReader.builder()
    .fieldSeparator(',')
    .quoteCharacter('"')
    .skipEmptyLines(true)
    .ignoreDifferentFieldCount(true);   
```

#### Reading data from file

Old way:
```java
try (CsvParser csvParser = new CsvReader().parse(file, UTF_8)) {
    CsvRow row;
    while ((row = csvParser.nextRow()) != null) {
        System.out.println("First field of row: " + row.getField(0));
    }
}
```

New way:
```java
try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(file)) {
    csv.forEach(rec ->
        System.out.println("First field of record: " + rec.getField(0))
    );
}
```

#### Reading data with a header from file

Old way:
```java
CsvReader csvReader = new CsvReader();
csvReader.setContainsHeader(true);
try (CsvParser csvParser = csvReader.parse(file, UTF_8)) {
    CsvRow row;
    while ((row = csvParser.nextRow()) != null) {
        System.out.println("Field named firstname: " + row.getField("firstname"));
    }
}
```

New way:
```java
try (CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(file)) {
    csv.forEach(rec ->
        System.out.println("Field named firstname: " + rec.getField("firstname"))
    );
}
```

#### Read an entire file at once

Old way:
```java
CsvContainer csv = new CsvReader().read(file, UTF_8);
```

New way:

The container concept has been removed, but you can
easily build something similar using the Java Stream API.
```java
List<CsvRecord> data;
try (CsvReader<CsvRecord> csvReader = CsvReader.builder().ofCsvRecord(file)) {
    data = csvReader.stream().toList();
}
```

### Writer

#### Configuring the writer

Old way:
```java
CsvWriter csvWriter = new CsvWriter();
csvWriter.setFieldSeparator(',');
csvWriter.setTextDelimiter('"');
csvWriter.setAlwaysDelimitText(true);
csvWriter.setLineDelimiter(new char[]{'\r','\n'});
```

New way:
```java
CsvWriter.builder()
    .fieldSeparator(',')
    .quoteCharacter('"')
    .quoteStrategy(QuoteStrategies.ALWAYS)
    .lineDelimiter(LineDelimiter.CRLF);
```

:::caution
Also note that the default line delimiter has changed!
In version 1.x the line delimiter was set based on system default `System.lineSeparator()`.
In version 2 and later the default is `\r\n` as defined in RFC 4180.
:::

#### Write to file

Old way:
```java
try (CsvAppender csvAppender = new CsvWriter().append(file)) {
    csvAppender.appendLine("header1", "header2");
    csvAppender.appendLine("value1", "value2");
}
```

New way:
```java
try (CsvWriter csvWriter = CsvWriter.builder().build(file)) {
    csvWriter
        .writeRecord("header1", "header2")
        .writeRecord("value1", "value2");
}
```

#### Write to writer

Old way:
```java
Writer writer = new StringWriter();
try (CsvAppender csvAppender = new CsvWriter().append(writer)) {
    csvAppender.appendLine("header1", "header2");
    csvAppender.appendLine("value1", "value2");
}
```

New way:
```java
Writer writer = new StringWriter();
try (CsvWriter csvWriter = CsvWriter.builder().build(writer)) {
    csvWriter
        .writeRecord("header1", "header2")
        .writeRecord("value1", "value2");
}
```

:::caution
Be aware of a change in the semantic in FastCSV.

In version 3.x you probably want to pass in a `java.io.BufferedWriter` for proper
performance. The opposite was recommended in version 1.x.
Check the Javadoc for further information.
:::
