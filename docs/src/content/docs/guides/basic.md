---
title: Basic Tutorial
sidebar:
  order: 2
---

This section covers the most basic usage of FastCSV. It provides examples for reading and writing CSV data.
For more advanced features, see the dedicated [Examples](/guides/examples/) section.

## CsvReader examples

### Iterative reading of some CSV data from a string

```java
CsvReader.builder().ofCsvRecord("foo1,bar1\nfoo2,bar2")
    .forEach(System.out::println);
```

### Iterative reading of a CSV file

```java
try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(file)) {
    csv.forEach(System.out::println);
}
```

### Iterative reading of some CSV data with a header

```java
CsvReader.builder().ofNamedCsvRecord("header 1,header 2\nfield 1,field 2")
    .forEach(rec -> System.out.println(rec.getField("header 2")));
```

### Iterative reading of some CSV data with a custom header

```java
CsvCallbackHandler<NamedCsvRecord> callbackHandler =
    new NamedCsvRecordHandler("header 1", "header 2");

CsvReader.builder().build(callbackHandler, "field 1,field 2")
    .forEach(rec -> System.out.println(rec.getField("header 2")));
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
    .acceptCharsAfterQuotes(false)
    .detectBomHeader(false);
```

## IndexedCsvReader examples

### Indexed reading of a CSV file

```java
try (IndexedCsvReader<CsvRecord> csv = IndexedCsvReader.builder().ofCsvRecord(file)) {
    CsvIndex index = csv.getIndex();

    System.out.println("Items of last page:");
    int lastPage = index.getPageCount() - 1;
    List<CsvRecord> csvRecords = csv.readPage(lastPage);
    csvRecords.forEach(System.out::println);
}
```

## CsvWriter examples

### Iterative writing of some data to a writer

```java
var sw = new StringWriter();
CsvWriter.builder().build(sw)
    .writeRecord("header 1", "header 2")
    .writeRecord("value 1", "value 2");

System.out.println(sw);
```

### Iterative writing of a CSV file

```java
try (CsvWriter csv = CsvWriter.builder().build(file)) {
    csv
        .writeRecord("header 1", "header 2")
        .writeRecord("value 1", "value 2");
}
```

### Custom settings

```java
CsvWriter.builder()
    .fieldSeparator(',')
    .quoteCharacter('"')
    .quoteStrategy(QuoteStrategies.ALWAYS)
    .commentCharacter('#')
    .lineDelimiter(LineDelimiter.LF);
```
