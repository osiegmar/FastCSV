---
title: Migrating from 3.x
sidebar:
  order: 5
---

This document only describes the **breaking** changes when migrating from FastCSV 3.x to 4.x.
For a full list of changes, including new features, see the [changelog](https://github.com/osiegmar/FastCSV/blob/main/CHANGELOG.md).

## Requirement changes

- The minimum Java version is now 17 (compared to 11 in FastCSV 3.x)
- This also raised the required Android API level from version 33 (Android 13) to 34 (Android 14)

## Record wrapper removal

The `RecordWrapper` class has been removed. It was a wrapper around the `CsvRecord` class that was used to provide parsing context information to the reading process.

If you implemented a custom callback handler by extending `AbstractBaseCsvCallbackHandler`, all you need to do is to return the `CsvRecord` instance directly instead of wrapping it in a `RecordWrapper`.

If you implemented a custom callback handler by implementing the `CsvCallbackHandler` interface, you also have to implement three additional methods: `isComment`, `isEmptyLine` and `getFieldCount`. Those methods simply have to return the information that was previously provided by the `RecordWrapper` instance.

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
