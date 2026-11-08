---
title: Architecture
description: This document describes the architecture of FastCSV.
---

This document describes the architecture of FastCSV.
It is intended for developers who want to understand the inner workings of the library.

The CSV terms used here (record, field, line break, …) are defined in the [CSV glossary](/glossary/).

## Reading CSV

Reading CSV is a complex process that requires several components to work together. The following sections provide an
overview of the components involved and the procedural flow.

### Component overview

#### CsvReader

The `CsvReader` serves as the central entry point for reading CSV data, orchestrating the entire process. Configured
through the builder pattern, it provides flexibility in setting up CSV format specifications and parsing behavior.
Utilizing the `CsvParser` for data parsing and the `CsvCallbackHandler` for field and record materialization,
the `CsvReader` returns CSV records to the user via the `Iterable` interface methods.

#### CsvReaderBuilder

The `CsvReaderBuilder` facilitates the configuration of the `CsvReader`, allowing users to specify CSV format details
and parsing behavior. Its primary purpose is to instantiate the `CsvParser` and `CsvReader` with the given settings.

#### CsvParser

The `CsvParser` encapsulates the low-level logic for parsing CSV data, acting under the control of the `CsvReader`. This
component is not intended for direct use. For each field, the `CsvParser` invokes the `CsvCallbackHandler` to process
and materialize it. Additionally, the `CsvParser` is responsible for handling line breaks.

#### CsvCallbackHandler

Serving as the interface between the `CsvReader` and the `CsvParser`, the `CsvCallbackHandler` is responsible for
processing and materializing fields. It initiates the materialization of records as soon as all fields of a record are
parsed. Using the `FieldModifier`, the `CsvCallbackHandler` can optionally modify fields, such as trimming or
altering case, before their materialization.
One implementation of the `CsvCallbackHandler` is the `CsvRecordHandler` which materializes records as
`CsvRecord` objects.

#### FieldModifier

The `FieldModifier` is employed to modify fields, providing capabilities such as trimming or altering case.

#### CsvRecord

The `CsvRecord` represents a single CSV record, providing access to its fields.
It is built by the `CsvRecordHandler`.

### Logic

The procedural flow can be outlined as follows:

- The `CsvReader` is configured using `CsvReaderBuilder`.
- Users either provide a custom `CallbackHandler` to the `CsvReaderBuilder` or rely on a default implementation.
- The `CsvReaderBuilder` controls the instantiation of the `CsvParser` and `CsvReader` with the given settings and
  opened CSV file. It returns the `CsvReader` which implements the `Iterable` interface.
- During each iteration, the `CsvReader` reads the next record from the CSV file by invoking the `CsvParser`.
- The `CsvParser`, in turn, passes each field to the `CsvCallbackHandler` until the end of the record is reached.
- For every field received, the `CsvCallbackHandler` invokes the `FieldModifier` to potentially modify the field and
  stores the field in a temporary buffer.
- The `CsvReader` calls the `CsvCallbackHandler` to materialize its buffer into a record (e.g., a `CsvRecord`) and
  returns it to the user.

### Usage

Basic usage of the `CsvReader` is demonstrated below.

```java
try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(file)) {
    csv.forEach(System.out::println);
}
```

This very basic example hides all the complexity that is going on behind the scenes.

- The `CsvReaderBuilder` is instantiated with default settings by the `CsvReader.builder()` factory method.
- The `ofCsvRecord()` method initializes a `CsvRecordHandler` with default settings, opens the given CSV file,
  initializes the `CsvParser` and `CsvReader`, and returns the `CsvReader` to the user.
- The user iterates over the `Iterable` by calling `forEach()` on it.
- As the `CsvReader` also implements `AutoCloseable`, the user can rely on the wrapping try-with-resources statement to
  close the opened file.

Exactly the same result but more explicitly is achieved by the following code.

```java
// Configures a reusable CsvReaderBuilder with default, but explicitly defined settings.
CsvReaderBuilder builder = CsvReader.builder()
    .fieldSeparator(',')
    .quoteCharacter('"');

// Use a "no operation" FieldModifier, which does not modify fields.
FieldModifier fieldModifier = FieldModifiers.NOP;

// Initializes a callback handler for CsvRecord objects and set the field modifier.
NamedCsvRecordHandler callbackHandler = NamedCsvRecordHandler.of(c -> c.
    .fieldModifier(fieldModifier)
);

// Use the builder to instantiate a CsvReader while passing the callback handler and the CSV file.
try (CsvReader<CsvRecord> csv = builder.build(callbackHandler, file)) {
    for (CsvRecord record : csv) {
        System.out.println(record);
    }
}
```

## Writing CSV

In contrast to reading CSV, writing CSV is a much simpler process and requires fewer components. The following sections
provide an overview of the components involved and the procedural flow.

### Component overview

#### CsvWriter

The `CsvWriter` serves as the central entry point for writing CSV data, orchestrating the entire process. Configured
through the builder pattern, it provides flexibility in setting up CSV format specifications and writing behavior.

#### CsvWriterBuilder

The `CsvWriterBuilder` facilitates the configuration of the `CsvWriter`, allowing users to specify CSV format details
and writing behavior. Its primary purpose is to instantiate the `CsvWriter` with the given settings.

#### QuoteStrategy

The `QuoteStrategy` is used to determine whether a field that does not require quoting (per CSV specification)
should be quoted. This can be used to enforce quoting for all fields, for example.

### Logic

The procedural flow can be outlined as follows:

- The `CsvWriter` is configured using `CsvWriterBuilder`.
- The `CsvWriterBuilder` controls the instantiation of the `CsvWriter` with the given settings and opened CSV file.
- The `CsvWriter` writes the given records to the CSV file, quoting fields as necessary (depending on the configured
  `QuoteStrategy`).
- The `CsvWriter` closes the CSV file.

### Usage

Basic usage of the `CsvWriter` is demonstrated below.

```java
try (CsvWriter csv = CsvWriter.builder().build(file)) {
    csv.writeRecord("field 1", "field 2");
}
```

This very basic example hides all the complexity that is going on behind the scenes.

- The `CsvWriterBuilder` is instantiated with default settings by the `CsvWriter.builder()` factory method.
- The `build()` method opens the given CSV file, initializes the `CsvWriter`, and returns it to the user.
- The user writes a record to the CSV file by calling `writeRecord()` on the `CsvWriter`.
- As the `CsvWriter` also implements `AutoCloseable`, the user can rely on the wrapping try-with-resources statement to
  close the opened file.

Exactly the same result but more explicitly is achieved by the following code.

```java
CsvWriterBuilder builder = CsvWriter.builder()
    .fieldSeparator(',')
    .quoteCharacter('"');

try (CsvWriter csv = builder.build(file)) {
    csv.writeRecord("field 1", "field 2");
}
```
