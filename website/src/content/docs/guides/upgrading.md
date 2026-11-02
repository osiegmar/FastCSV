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

## Duplicate field names

FastCSV 4.x rejects duplicate field names in the header by default, ensuring that each field name is unique and preventing misinterpretation.

You can change this behavior by calling `allowDuplicateHeaderFields(true)` on the `NamedCsvRecordHandlerBuilder`.

```java title="Example"
var rh = NamedCsvRecordHandler.of(c -> c.allowDuplicateHeaderFields(true));
try (CsvReader<NamedCsvRecord> csv = CsvReader.builder().build(rh, csvFile)) {
    // ...
}
```

:::caution
As the default has changed, you may need to check your code and your desired behavior.
:::

## Ignoring different field counts

FastCSV 4.x no longer ignores different field counts by default, ensuring that data is not misinterpreted.

You can change this behavior by calling `extraFieldStrategy(FieldMismatchStrategy.IGNORE)` and `missingFieldStrategy(FieldMismatchStrategy.IGNORE)` in the `CsvReaderBuilder`.
These methods provide more control over how to handle different field counts in CSV data than the previous (now removed) `ignoreDifferentFieldCount()` method.

```java title="Example"
CsvReaderBuilder builder = CsvReader.builder()
    .extraFieldStrategy(FieldMismatchStrategy.IGNORE)
    .missingFieldStrategy(FieldMismatchStrategy.IGNORE);

try (CsvReader<CsvRecord> csv = builder.ofCsvRecord(csvFile)) {
    // ...
}
```

:::caution
As the default has changed, you may need to check your code and your desired behavior.
:::

## Internal buffer flushing

In FastCSV 2.x and 3.x, the CsvWriter instantiated via `CsvWriterBuilder.build(Writer)` flushed the internal buffer to the `Writer` after each record.

**This is no longer the case!**

In FastCSV 4.x, the `CsvWriter` for `OutputStream` and `Writer` behaves the same way:
it writes the internal buffer to the `OutputStream` or `Writer` only when the internal buffer is full, or when `flush()` or `close()` is called.
The buffer can also be completely disabled by calling `CsvWriterBuilder.bufferSize(0)`.

As a consequence, you don't need to wrap the `Writer` in a `BufferedWriter` for good performance, unless you have disabled FastCSV's internal buffer (`CsvWriterBuilder.bufferSize(0)`).

This change was made to align with the behavior of `CsvWriter` for `OutputStream`. When writing to an `OutputStream`, a `java.io.OutputStreamWriter` is used, which also buffers internally. Automatically flushing FastCSV's internal buffer after each record would have no effect, as the `OutputStreamWriter` would still buffer the data until it is full or flushed manually.

**Summary, when dealing with `Writer` or `OutputStream`:**
- Close the `CsvWriter` at the end of your writing process to ensure all data is written.
- Call `flush()` on the `CsvWriter` before writing to the underlying `Writer` or `OutputStream` directly to ensure all data is written.
- Calling `CsvWriterBuilder.bufferSize(0)` disables FastCSV's internal buffer, but the buffer of `java.io.OutputStreamWriter` cannot be disabled.
- To get the data to the `Writer` or `OutputStream` after each record, as in 3.x, call `CsvWriterBuilder.autoFlush(true)`.
  It writes the internal buffer and calls `flush()` on the `Writer` or `OutputStream` after each record.

:::caution
As the default has changed, you may need to check your code and your desired behavior.
:::

## Handling of characters after the closing quote character

FastCSV 4.x no longer tolerates characters after the closing quote character of a quoted field, by default.
This is a change in behavior compared to earlier versions of FastCSV. It ensures no erroneous data is read and interpreted without noticing it.

The following examples will now lead to a `CsvParseException`:
```csv
"foo"INVALID,"bar"
"foo" ,"bar"
```

To restore the old behavior, you can use the `allowExtraCharsAfterClosingQuote(boolean)` method in `CsvReaderBuilder` or `IndexedCsvReaderBuilder`.
This method was named `acceptCharsAfterQuotes(boolean)` in earlier versions of FastCSV.
Note that `allowExtraCharsAfterClosingQuote` is deprecated and will be removed in a future major release.
To tolerate whitespace around quoted fields, use `CsvReaderBuilder.trimWhitespacesAroundQuotes(boolean)` instead.
Unlike the old behavior, it removes the whitespace: `"foo" ,"bar"` is read as `foo` and `bar` instead of `foo ` and `bar`.
This option is not available for `IndexedCsvReader`.

:::caution
As the default has changed, you may need to check your code and your desired behavior.
:::

## Quote strategies

Quote strategies received two breaking changes in FastCSV 4:

- The method `quoteNonEmpty` of `de.siegmar.fastcsv.writer.QuoteStrategy` has been renamed to `quoteValue` to better reflect its purpose.
  As it is a default method, an implementation that overrides `quoteNonEmpty` without `@Override` still compiles, but the method is no longer called.
  Rename it to `quoteValue`.
- To explicitly define that quoting only happens if required, the `quoteStrategy` method in `CsvWriterBuilder` no longer accepts a `null` value.
  Instead, you can use the `QuoteStrategies.REQUIRED` constant to indicate that quoting should only happen if required.

## Changed implementation of `CsvIndex` to a Java record

`CsvIndex` and its nested `CsvPage` have been changed to Java records.
Of their methods, only `getPageCount()` and `getRecordCount()` were public in 3.x; they have changed as follows:

```diff lang="java"
  CsvIndex csvIndex = yourCodeToBuildTheIndex();

- csvIndex.getPageCount();
+ csvIndex.pages().size();

- csvIndex.getRecordCount();
+ csvIndex.recordCount();
```

If you serialized a `CsvIndex` with FastCSV 3.x, build it again with 4.x.
A deserialized 3.x index has a record count of 0 and is rejected by `IndexedCsvReaderBuilder.index(CsvIndex)` with an `IllegalArgumentException`.

## Callback handler refactoring

If you implemented a custom callback handler, some major changes have been made in FastCSV 4.0 that you need to be aware of:

**Record wrapper removal**: The `RecordWrapper` class has been removed (remove any `import de.siegmar.fastcsv.reader.RecordWrapper` statements). Its responsibilities have been absorbed by the `CsvCallbackHandler`:
- `RecordWrapper.getFieldCount()` → new abstract method `CsvCallbackHandler.getFieldCount()`
- `RecordWrapper.isComment()` / `isEmptyLine()` → new abstract method `CsvCallbackHandler.getRecordType()` returning a `RecordType` enum
- `buildRecord()` now returns `T` directly (or `null` to skip) instead of `RecordWrapper<T>`

**Handling empty lines**: The CSV parser now emits empty lines via a dedicated `setEmpty()` callback, separately from comments and regular records.

### Migrating `AbstractBaseCsvCallbackHandler` subclasses

If you extended `AbstractBaseCsvCallbackHandler`, your `buildRecord` method must now return the record directly instead of wrapping it, and `isComment()`/`isEmptyLine()` are replaced by `getRecordType()`. You may also want to override `handleEmpty()` to handle empty lines.

```diff lang="java"
  @Override
- protected RecordWrapper<MyRecord> buildRecord() {
-     if (isComment()) {
+ protected MyRecord buildRecord() {
+     if (getRecordType() == RecordType.COMMENT) {
          // handle comment...
      }
-     if (isEmptyLine()) {
+     if (getRecordType() == RecordType.EMPTY) {
          // handle empty line...
      }
      MyRecord record = new MyRecord(/* ... */);
-     return wrapRecord(record);
+     return record;
  }
```

### Other callback handler classes

In 3.x, a direct subclass of `CsvCallbackHandler` outside FastCSV could not create a `RecordWrapper`, as its constructor was not public.
In 4.x, you can extend `CsvCallbackHandler` directly; it requires the methods `getRecordType()`, `getFieldCount()`, and `setEmpty()`.
See [custom callback handler](/guides/examples/custom-callback-handler/) for an example.

`AbstractInternalCsvCallbackHandler` was documented as internal in 3.x; in 4.x it is sealed and can no longer be extended.
Extend `AbstractBaseCsvCallbackHandler` instead.

## Null-free refactoring in CollectingStatusListener

The method `getThrowable` of `CollectingStatusListener` no longer returns `null` when no exception has occurred. Instead, it returns an `Optional<Throwable>`. This aligns with the null-free design of FastCSV.

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
In version 3.6.0 these properties were deprecated in favor of the builder methods of `CsvReaderBuilder` and of the callback handlers (`CsvRecordHandler`, `NamedCsvRecordHandler`, `StringArrayHandler`).
In version 4.0.0 those deprecated properties were removed.

In 3.x, `fastcsv.max.field.size` set the maximum field size, set the maximum record size to four times that value, and limited the maximum buffer size to that value.
The following example sets the same limits as the system properties in 3.x:

```diff lang="java"
- System.setProperty("fastcsv.max.field.count", "1024");
- System.setProperty("fastcsv.max.field.size", "1048576");
+ CsvRecordHandler handler = CsvRecordHandler.builder()
+     .maxFields(1_024)
+     .maxFieldSize(1_048_576)
+     .maxRecordSize(4_194_304)
+     .build();
+
+ CsvReader<CsvRecord> csvReader = CsvReader.builder()
+     .maxBufferSize(1_048_576)
+     .build(handler, file);
```

### FieldModifiers.modify moved to FieldModifier.modify

The `FieldModifiers.modify(Function)` method has been moved to the `FieldModifier` interface.
This also replaces the `SimpleFieldModifier` class, which was deprecated in 3.7.0 and removed in 4.0.0.

```diff lang="java"
  // If you used SimpleFieldModifier (deprecated since 3.7.0):
  FieldModifier normalizeWhitespaces =
-     (SimpleFieldModifier) field -> field.replaceAll("\\s", " ");
+     FieldModifier.modify(field -> field.replaceAll("\\s", " "));

  // If you used FieldModifiers.modify (added in 3.7.0):
  FieldModifier normalizeWhitespaces =
-     FieldModifiers.modify(field -> field.replaceAll("\\s", " "));
+     FieldModifier.modify(field -> field.replaceAll("\\s", " "));
```

### Upper and lower case field modifiers

The edge-case methods `FieldModifiers.lower(Locale)` and `FieldModifiers.upper(Locale)` were deprecated in 3.7.0 and removed in 4.0.0.

Use `FieldModifier.modify` instead, which has moved from `FieldModifiers` to the `FieldModifier` interface.

```diff lang="java"
  FieldModifier toLowerFieldModifier =
-     FieldModifiers.lower(Locale.ENGLISH);
+     FieldModifier.modify(field -> field.toLowerCase(Locale.ENGLISH));

  FieldModifier toUpperFieldModifier =
-     FieldModifiers.upper(Locale.ENGLISH);
+     FieldModifier.modify(field -> field.toUpperCase(Locale.ENGLISH));
```

## Changes in later 4.x releases

If you upgrade from 3.x directly to the latest 4.x version, these changes from 4.x minor and patch releases also apply.

### Charsets supported by `IndexedCsvReader` (4.3.1, 4.4.0)

`IndexedCsvReader` builds its index on byte level.
Since 4.4.0, it accepts only UTF-8 and single-byte charsets that encode ASCII characters as the same single bytes (such as ISO-8859-1 or windows-1252).
Other charsets, such as UTF-16 or Shift_JIS, lead to an `IllegalArgumentException`.
For CSV data in such a charset, use `CsvReader` or transcode the data to UTF-8.

### `skipLines` only before reading records (4.3.0)

`CsvReader.skipLines(int)` and `CsvReader.skipLines(Predicate, int)` are meant to skip lines before the CSV data starts.
Since 4.3.0, calling them after the first record has been read leads to an `IllegalStateException`.

### Unwrapped `CsvParseException` (4.4.0)

In 3.x, a `CsvParseException` thrown while parsing was wrapped in another `CsvParseException` that added the starting line number.
Since 4.4.0, it is thrown as is, and its message contains the starting line number.
If your code reads the cause of a `CsvParseException`, check it.
