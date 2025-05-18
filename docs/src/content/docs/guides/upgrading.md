---
title: Upgrading
description: Guide on how to upgrade from earlier versions of FastCSV to the latest version.
sidebar:
  order: 3
---

This document describes the **breaking** changes when upgrading from FastCSV 3.x to 4.x.
For a full list of changes, including new features, see the [changelog](https://github.com/osiegmar/FastCSV/blob/main/CHANGELOG.md).

## Requirement changes

- The minimum Java version has been raised from 11 to 17
- This also raised the required Android API level from version 33 (Android 13) to 34 (Android 14)

## Internal buffer flushing

In FastCSV 2.x and 3.x, the CsvWriter instantiated via `CsvWriterBuilder.build(Writer)` flushed the internal buffer to the `Writer` after each record.

**This is no longer the case!**

In FastCSV 4.x, the `CsvWriter` for `OutputStream` and `Writer` behaves the same way:
it writes the internal buffer to the `OutputStream` or `Writer` only when the internal buffer is full, or when `flush()` or `close()` is called.
The buffer can also be completely disabled by calling `CsvWriterBuilder.bufferSize(0)`.

As a consequence, you no longer need or should wrap the `Writer` in a `BufferedWriter` to ensure proper performance, unless you haven't disabled FastCSV's internal buffer (`CsvWriterBuilder.bufferSize(0)`).

This change was made to align with the behavior of `CsvWriter` for `OutputStream`. When writing to an `OutputStream`, a `java.io.OutputStreamWriter` is used, which also buffers internally. Automatically flushing FastCSV's internal buffer after each record would have no effect, as the `OutputStreamWriter` would still buffer the data until it is full or flushed manually.

**Summary, when dealing with `Writer` or `OutputStream`:**
- Close the `CsvWriter` at the end of your writing process to ensure all data is written.
- Call `flush()` on the `CsvWriter` before writing to the underlying `Writer` or `OutputStream` directly to ensure all data is written.
- Calling `CsvWriterBuilder.bufferSize(0)` disables FastCSV's internal buffer, but the buffer of `java.io.OutputStreamWriter` cannot be disabled.

:::caution
As the default has changed, you may need to check your code and your desired behavior.
:::

## Changed implementation of `CsvIndex` and `CsvPage` to Java records

The `CsvIndex` and `CsvPage` classes have been changed to Java records. With this change, a few method calls have changed as well.

```diff lang="java"
  CsvIndex csvIndex = yourCodeToBuildTheIndex();

  // Simple get-prefix removal
- csvIndex.getBomHeaderLength();
+ csvIndex.bomHeaderLength();

- csvIndex.getFileSize();
+ csvIndex.fileSize();

- csvIndex.getFieldSeparator();
+ csvIndex.fieldSeparator();

- csvIndex.getQuoteCharacter();
+ csvIndex.quoteCharacter();

- csvIndex.getCommentStrategy();
+ csvIndex.commentStrategy();

- csvIndex.getCommentCharacter();
+ csvIndex.commentCharacter();

- csvIndex.getRecordCount();
+ csvIndex.recordCount();

  // Replaced getPageCount() with pages().size()
- csvIndex.getPageCount();
+ csvIndex.pages().size();

  // Replaced getPage(int) with direct access to the pages list
- var firstPage = csvIndex.getPage(0);
+ var firstPage = csvIndex.pages().getFirst();

  // Again, simple get-prefix removal
- firstPage.getOffset();
+ firstPage.offset()

- firstPage.getStartingLineNumber();
+ firstPage.startingLineNumber();
```

## Record wrapper removal

The `RecordWrapper` class has been removed. It was a wrapper around the `CsvRecord` class that was used to provide parsing context information to the reading process.

If you implemented a custom callback handler by extending `AbstractBaseCsvCallbackHandler`, all you need to do is to return the `CsvRecord` instance directly instead of wrapping it in a `RecordWrapper`.

If you implemented a custom callback handler by implementing the `CsvCallbackHandler` interface, you also have to implement three additional methods: `isComment`, `isEmptyLine` and `getFieldCount`. Those methods simply have to return the information that was previously provided by the `RecordWrapper` instance.

## Removed deprecated code

In FastCSV 4.0 some deprecated code was removed.

### Callback handler instantiation

The constructors of `CsvRecordHandler`, `NamedCsvRecordHandler`, and `StringArrayHandler` have been deprecated in 3.6.0 and now have been removed in 4.0.0.

Just use the builder methods instead:

```diff lang="java"
  NamedCsvRecordHandler defaultHandler =
-     new NamedCsvRecordHandler();
+     NamedCsvRecordHandler.of();

  NamedCsvRecordHandler predefinedHeaderWithTrim =
-     new NamedCsvRecordHandler(FieldModifiers.TRIM, "foo", "bar");
+     NamedCsvRecordHandler.builder()
+         .fieldModifier(FieldModifiers.TRIM)
+         .header("foo", "bar")
+         .build();

  // ... or functional-style
  NamedCsvRecordHandler predefinedHeaderWithTrim =
-     new NamedCsvRecordHandler(FieldModifiers.TRIM, "foo", "bar");
+     NamedCsvRecordHandler.of(builder -> builder
+         .fieldModifier(FieldModifiers.TRIM)
+         .header("foo", "bar")
+     );
```

This change was necessary because callback handlers now have more configuration options, making constructor initialization impractical.

### Configuring limits

In FastCSV 3.2.0, the default limits for the maximum number of fields per record and the maximum field size were made configurable
via system properties `fastcsv.max.field.count` and `fastcsv.max.field.size`.
In version 3.6.0 these properties were deprecated in favor of a more flexible configuration via the builder methods of `CsvReader` and `CallbackHandler`.
In version 4.0.0 those deprecated properties were removed.

```diff lang="java"
  // Set the maximum number of fields per record
- System.setProperty("fastcsv.max.field.count", "16384");
+ CsvRecordHandler handler = CsvRecordHandler.builder()
+     .maxFields(16_384)
+     .maxFieldSize(16_777_216)
+     .build();

  // Set the maximum buffer size and maximum field size
- System.setProperty("fastcsv.max.field.size", "16777216");
+ CsvReader csvReader = CsvReader.builder()
+    .maxBufferSize(16_777_216)
+    .build(handler, file);
```

### SimpleFieldModifier

The `SimpleFieldModifier` class has been deprecated in 3.7.0 and removed in 4.0.0.

Use `FieldModifiers.modify` instead.

```diff lang="java"
  FieldModifier normalizeWhitespaces =
-     (SimpleFieldModifier) field -> field.replaceAll("\\s", " ");
+     FieldModifiers.modify(field -> field.replaceAll("\\s", " "));
```

### Upper and lower case field modifiers

The edge-case methods `FieldModifiers.lower(Locale)` and `FieldModifiers.upper(Locale)` were deprecated in 3.7.0 and removed in 4.0.0.

Use `FieldModifiers.modify` instead.

```diff lang="java"
  FieldModifier toLowerFieldModifier =
-     FieldModifiers.lower(Locale.ENGLISH);
+     FieldModifiers.modify(field -> field.toLowerCase(Locale.ENGLISH));

  FieldModifier toUpperFieldModifier =
-     FieldModifiers.upper(Locale.ENGLISH);
+     FieldModifiers.modify(field -> field.toUpperCase(Locale.ENGLISH));
```
