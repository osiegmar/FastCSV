---
title: Migrating from 2.x
sidebar:
  order: 4
---

This document only describes the **breaking** changes when migrating from FastCSV 2.x to 3.x.
For a full list of changes, including new features, see the [changelog](https://github.com/osiegmar/FastCSV/blob/main/CHANGELOG.md).

## Requirement changes

- The minimum Java version is now 11 (compared to 8 in earlier versions)
- This also raised the required Android API level from version 26 (Android 8) to 33 (Android 13)

## New limitations

- The maximum number of fields per record is now limited to 16,384 to avoid `OutOfMemoryErrors` caused
  by excessive field counts.
- The maximum record size is now limited to (64 MiB) to prevent `OutOfMemoryErrors`.

## Naming changes

### Rows are now called Records (aligned to RFC 4180)

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

### Method names

- In `CsvReaderBuilder`:
    - `skipEmptyRows` is now `skipEmptyLines`
    - `errorOnDifferentFieldCount` is now `ignoreDifferentFieldCount` (opposite meaning!)
    - `build` methods with callback handlers and `ofCsvRecord` / `ofNamedCsvRecord` as convenience methods
- In `CsvRecord` (former `CsvRow`):
    - `getOriginalLineNumber` is now `getStartingLineNumber`

## NamedCsvReader removed/replaced

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

## Extended/Refactored quote strategies

- `QuoteStrategy` changed from an enum to an interface (see `QuoteStrategies` for the default implementations)
- The `REQUIRED` quote strategy is removed as it is the default (no quote strategy is needed)

**Example to enable always quoting:**

```java
CsvWriter.builder()
    .quoteStrategy(QuoteStrategies.ALWAYS);
```

## Exception changes

`MalformedCsvException` is now `CsvParseException` and is thrown instead of `IOException` for non-IO related errors.
