# Upgrading to 3.0

This document shows a list of breaking changes in version 3 and how to upgrade.

- Java version requirement raised from version 8 to 11
- This also raised the required Android API level from version 26 (Android 8) to 33 (Android 13)
- Rows are now called Records (aligned to RFC 4180)

If you're still on version 1, read on.

## Reader

### Configuring the reader

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
    .ignoreDifferentFieldCount(false);
```

### Reading data from file

Old way:
```java
try (CsvParser csvParser = new CsvReader().parse(file, UTF_8)) {
    CsvRow row;
    while ((row = csvParser.nextRow()) != null) {
        System.out.println("First field of record: " + row.getField(0));
    }
}
```

New way:
```java
try (CsvReader csvReader = CsvReader.builder().build(path)) {
    csvReader.forEach(csvRecord ->
        System.out.println("First field of record: " + csvRecord.getField(0))
    );
}
```

### Reading data with a header from file

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
try (NamedCsvReader csvReader = NamedCsvReader.builder().build(path)) {
    csvReader.forEach(csvRecord ->
        System.out.println("Field named firstname: " + csvRecord.getField("firstname"))
    );
}
```

### Read an entire file at once

Old way:
```java
CsvContainer csv = new CsvReader().read(file, UTF_8);
```

New way:
The container concept has been removed, but you can
easily build something similar using the Java Stream API.
```java
List<CsvRecord> data;
try (CsvReader csv = CsvReader.builder().build(path)) {
    data = csv.stream().toList();

    // or in Java < 16:
    // data = csv.stream().collect(Collectors.toList());
}
```

## Writer

### Configuring the writer

Old way:
```java
CsvWriter csvWriter = new CsvWriter();
csvWriter.setFieldSeparator(',');
csvWriter.setTextDelimiter('"');
csvWriter.setAlwaysDelimitText(false);
csvWriter.setLineDelimiter(new char[]{'\r','\n'});
```

New way:
```java
CsvWriter.builder()
    .fieldSeparator(',')
    .quoteCharacter('"')
    .quoteStrategy(QuoteStrategy.REQUIRED)
    .lineDelimiter(LineDelimiter.CRLF);
```

> :warning: Also note that the default line delimiter has changed!
> In version 1.x the line delimiter was set based on system default `System.lineSeparator()`.
> In version 2/3 the default is `\r\n` as defined in RFC 4180.

### Write to file

Old way:
```java
try (CsvAppender csvAppender = new CsvWriter().append(file)) {
    csvAppender.appendLine("header1", "header2");
    csvAppender.appendLine("value1", "value2");
}
```

New way:
```java
try (CsvWriter csv = CsvWriter.builder().build(file)) {
    csv
        .writeRecord("header1", "header2")
        .writeRecord("value1", "value2");
}
```

### Write to writer

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
try (CsvWriter csv = CsvWriter.builder().build(writer)) {
    csv
        .writeRecord("header1", "header2")
        .writeRecord("value1", "value2");
}
```

> :warning: Be aware of a change in the semantic in FastCSV.
> In version 2/3 you probably want to pass in a `java.io.BufferedWriter` for proper
> performance. The opposite was recommended in version 1.x.
> Check the Javadoc for further information.
