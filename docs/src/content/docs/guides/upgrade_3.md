---
title: Migrating from 3.x
sidebar:
  order: 5
---

This document only describes the **breaking** changes when migrating from FastCSV 3.x to 4.x.
For a full list of changes, including new features, see the [changelog](https://github.com/osiegmar/FastCSV/blob/main/CHANGELOG.md).

## Requirement changes

- The minimum Java version is now 17 (compared to 11 in FastCSV 3.x)
- When using on Android, the minimum version is now 34 (Android 14) (compared to 33 in FastCSV 3.x)

## Internal buffer flushing

In FastCSV 2.x, the CsvWriter instantiated via `CsvWriterBuilder#build(Writer)` flushed the internal buffer to the `Writer` after each record.

**This is no longer the case!**

In FastCSV 3.x, the CsvWriter for `OutputStream` and `Writer` behaves the same way: it writes the internal buffer to the `OutputStream` or `Writer` only when the internal buffer is full or when you call `flush()`. The buffer can be disabled by calling `CsvWriterBuilder#bufferSize(0)`.

As a consequence, you no longer need or should wrap the `Writer` in a `BufferedWriter` to ensure proper performance, unless you haven't disabled FastCSV's internal buffer (`CsvWriterBuilder#bufferSize(0)`).

:::caution
This is a tricky one, as the behavior of this method has changed but the API has not! Check your code to ensure that you are not relying on the old behavior.
:::

## Record wrapper removal

The `RecordWrapper` class has been removed. It was a wrapper around the `CsvRecord` class that was used to provide parsing context information to the reading process.

If you implemented a custom callback handler by extending `AbstractBaseCsvCallbackHandler`, all you need to do is to return the `CsvRecord` instance directly instead of wrapping it in a `RecordWrapper`.

If you implemented a custom callback handler by implementing the `CsvCallbackHandler` interface, you also have to implement three additional methods: `isComment`, `isEmptyLine` and `getFieldCount`. Those methods simply have to return the information that was previously provided by the `RecordWrapper` instance.

## Callback handler instantiation

The constructors of `CsvRecordHandler`, `NamedCsvRecordHandler`, and `StringArrayHandler` have been deprecated in 3.6.0 and now have been removed in 4.0.0.

Just use the builder methods instead:

Old way:
```java
CsvRecordHandler defaultHandler = new CsvRecordHandler();
CsvRecordHandler trimmingHandler = new CsvRecordHandler(FieldModifiers.TRIM);
```

New way:
```java
CsvRecordHandler defaultHandler = CsvRecordHandler.of();
CsvRecordHandler trimmingHandler = CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));

// or
CsvRecordHandler trimmingHandler = CsvRecordHandler.builder()
    .fieldModifier(FieldModifiers.TRIM)
    .build();
```

This change was necessary because callback handlers now have more configuration options, making constructor initialization impractical.

## Configuring limits

In FastCSV 3.2.0, the default limits for the maximum number of fields per record and the maximum field size were made configurable
via system properties `fastcsv.max.field.count` and `fastcsv.max.field.size`.
In version 3.6.0 these properties were deprecated in favor of a more flexible configuration via the CsvReader and CallbackHandler builder methods.
In version 4.0.0 those deprecated properties were removed.

Old way:
```java
// Set the maximum number of fields per record
System.setProperty("fastcsv.max.field.count", "16384");

// Set the maximum buffer size and maximum field size
System.setProperty("fastcsv.max.field.size", "16777216");
```

New way:
```java
CsvRecordHandler handler = CsvRecordHandler.builder()
    .maxFields(16_384)
    .maxFieldSize(16_777_216)
    .build();

CsvReader csvReader = CsvReader.builder()
    .maxBufferSize(16_777_216)
    .build(handler, file);
```

## SimpleFieldModifier

The `SimpleFieldModifier` class has been deprecated in 3.7.0 and are now removed in 4.0.0.

Old way:
```java
FieldModifier normalizeWhitespaces =
    (SimpleFieldModifier) field -> field.replaceAll("\\s", " ");
```

New way:
```java
FieldModifier normalizeWhitespaces =
    FieldModifiers.modify(field -> field.replaceAll("\\s", " "));
```

## Upper and lower case field modifiers

The edge-case methods `FieldModifiers#lower(Locale)` and `FieldModifiers#upper(Locale)` were deprecated in 3.7.0 and now have been removed in 4.0.0.

Old way:
```java
FieldModifier toLowerFieldModifier = FieldModifiers.lower(Locale.ENGLISH);
FieldModifier toUpperFieldModifier = FieldModifiers.upper(Locale.ENGLISH);
```

New way:
```java
FieldModifier toLowerFieldModifier = FieldModifiers.modify(field -> field.toLowerCase(Locale.ENGLISH));
FieldModifier toUpperFieldModifier = FieldModifiers.modify(field -> field.toUpperCase(Locale.ENGLISH));
```

## Changed implementation of `CsvIndex` and `CsvPage` to Java records

The `CsvIndex` and `CsvPage` classes have been changed to Java records. With this change, a few method calls have changed as well.

Old way:
```java
CsvIndex csvIndex = yourCodeToBuildTheIndex();

// Data types haven't changed, so omitting them here
var bomHeaderLength = csvIndex.getBomHeaderLength();
var fileSize = csvIndex.getFileSize();
var fieldSeparator = csvIndex.getFieldSeparator();
var quoteCharacter = csvIndex.getQuoteCharacter();
var commentStrategy = csvIndex.getCommentStrategy();
var commentCharacter = csvIndex.getCommentCharacter();
var recordCount = csvIndex.getRecordCount();
var pageCount = csvIndex.getPageCount();
var firstPage = csvIndex.getPage(0);
var offset = firstPage.getOffset();
var lineNumber = firstPage.getStartingLineNumber();
```

New way:
```java
CsvIndex csvIndex = yourCodeToBuildTheIndex();

// Simple get-prefix removal
var bomHeaderLength = csvIndex.bomHeaderLength();
var fileSize = csvIndex.fileSize();
var fieldSeparator = csvIndex.fieldSeparator();
var quoteCharacter = csvIndex.quoteCharacter();
var commentStrategy = csvIndex.commentStrategy();
var commentCharacter = csvIndex.commentCharacter();
var recordCount = csvIndex.recordCount();

// Replaced getPageCount() with pages().size()
var pageCount = csvIndex.pages().size();

// Replaced getPage(int) with direct access to the pages list
var firstPage = csvIndex.pages().getFirst();

// Again, simple get-prefix removal
var offset = firstPage.offset();
var lineNumber = firstPage.startingLineNumber();
```
