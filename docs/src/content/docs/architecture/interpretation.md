---
title: CSV Interpretation
description: How FastCSV interprets CSV data and how this interpretation aligns with the RFC specification.
---

At first, the CSV format may seem very simple. In practice, however, numerous edge cases need to be considered.
The [JavaCsvComparison](https://github.com/osiegmar/JavaCsvComparison) project illustrates that there are many different
ways to interpret CSV files, especially when not strictly following the RFC specification. Some of these differences
might be considered as bugs, while others represent alternative approaches to handling edge cases (mostly garbled data).

This document describes how FastCSV interprets CSV data and how this interpretation aligns with the RFC specification.

## Status of the RFC

[RFC 4180](https://datatracker.ietf.org/doc/html/rfc4180) dates back to 2005. Since then, notable changes have occurred.
For instance, it originally described common character set usage as US-ASCII and made no mention of Unicode. Nowadays,
UTF-8 has become the standard for CSV files.

Yakov Shafranovich, the original author of RFC 4180, and I (Oliver Siegmar, the author of FastCSV) conducted a thorough
review and revision of the RFC. Our focus was on addressing numerous edge cases that arise in practical scenarios.

The draft for RFC 4180-bis is available
on [datatracker.ietf.org](https://datatracker.ietf.org/doc/html/draft-shafranovich-rfc4180-bis), and you can explore the
GitHub repository at [osiegmar/rfc4180-bis](https://github.com/osiegmar/rfc4180-bis).

Currently, the draft is under review by the IETF.

## Implementation details

### Encoding

While RFC 4180 refers to US-ASCII, RFC 4180-bis uses Unicode (UTF-8).

FastCSV supports any encoding when reading or writing CSV files while defaulting to UTF-8.

### Empty fields / null values

The CSV format itself does not provide a way to distinguish between empty fields and null values. This can be a problem
when such a distinction is required.

FastCSV provides the following options to handle null values:

**on Writing**: depending on the configured `QuoteStrategy`, null values are either written as empty fields (default) or
as quoted empty fields (`QuoteStrategies.EMPTY`). This helps to produce PostgreSQL-compatible CSV files for example. If
you instead want to write a specific string for null values, you have to pass the string to
the `CsvWriter.writeRecord()` method.

**on Reading**: the `CsvCallbackHandler.addField()` method gets passed a parameter `quoted` that indicates whether the
field was quoted or not. The standard record implementation (`CsvRecord`) does currently not provide a way to
distinguish between empty fields and null values by itself but a custom implementation could use the `quoted` parameter
to do so.

Example for **empty** and **quoted empty** fields:

```
1,,fooCRLF
2,"",barCRLF
```

### Newline after last record

RFC 4180 says:
> The last record in the file may or may not have an ending line break.

RFC 4180-bis says:
> The last record in the file MUST have an ending line break indicating the end of a record.

This change was implemented to eliminate ambiguity, especially when the CSV file consists of only one field per record.
In such cases, it becomes impossible to determine whether the example has a third record (containing an empty field) or
not:

```
fooCRLF
barCRLF
```

FastCSV conforms to the RFC 4180-bis requirement and includes end-of-line characters after every record, including the
last one.

### Different field count

The RFC says:
> Each record SHOULD contain the same number of fields throughout the file.

In practice, however, this is not always the case. This prompts the question of what the correct behavior should be.

Consider the following CSV snippet as an illustration of varying field counts:

```
header_a,header_bCRLF
value_a_1CRLF
value_a_2,value_b_2,value_c_2CRLF
```

In this example, `value_a_1` likely belongs to `header_a`, and `header_b` does not have a value for the first data
record. However, this is just an assumption. Field `value_c_2` does not even have a corresponding header.

To ensure no misinterpretation, FastCSV does not allow extra or missing fields in a record by default.
This means that the above example would result in a `CsvParseException` when reading it with FastCSV.

However, this behavior can be changed by setting `CsvReaderBuilder.allowExtraFields(boolean)`
and `CsvReaderBuilder.allowMissingFields(boolean)` to `true`.

### Empty lines

As a consequence of not dictating equal field counts, a line could be completely empty. This is particularly relevant
for a CSV file with only one column, raising the question of whether the field is empty or if the line is empty and
should be skipped.

```
value_1CRLF
CRLF
value_2CRLF
```

By default, FastCSV skips empty lines (see `CsvReaderBuilder.skipEmptyLines(boolean)`). Reading such a file would result
in two records (containing `"value_1"` and `"value_2"`).

To regularly read empty lines, you can disable this behavior, resulting in three records (containing `"value_1"`, `""`,
and `"value_2"`).

### Empty files

Empty files (zero bytes) are valid per RFC. FastCSV will return an empty stream when reading such a file.

### Fields spanning multiple lines

Fields enclosed in double quotes can span multiple lines.

```
"a multi-lineCRLF
field"CRLF
```

FastCSV supports this for reading and writing. The Java value for this field would be `"a multi-line\r\nfield"`.
Any end-of-line characters within the field are preserved.

### Unique header names

The RFC says:
> Implementers should be aware that some applications may treat header values as unique
> (either case-sensitive or case-insensitive).

```
header_a,header_aCRLF
value_1,value_2CRLF
```

The `NamedCsvRecord` of FastCSV offers several options to handle this case:

- `getField("header_a")`, `findField("header_a")` and `getFieldsAsMap()` returns only the **first** value (`"value_1"`).
- `findFields("header_a")` and `getFieldsAsMapList()` returns a List containing **all** values (`"value_1"`
  and `"value_2"`).

Regardless of the chosen option, FastCSV always handles the header as case-sensitive.

### Spaces within fields

The RFC says:
> Spaces are considered part of a field and SHOULD NOT be ignored

FastCSV adheres to this recommendation by reading and writing fields as they are, including any (leading or trailing)
whitespaces.

When reading CSV files, you have the option to configure a `FieldModifier` to trim the field value before it is returned
to the user. The following example demonstrates trimming, printing `"foo"` and `"bar"` without any whitespaces:

```java
var handler = CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));
CsvReader.builder().build(handler, " foo , bar ")
    .forEach(System.out::println);
```

### Whitespace outside quoted fields

The RFC says:
> When quoted fields are used, this document does not allow whitespace between double quotes and commas.

FastCSV strictly adheres to this requirement: it never writes whitespaces between double quotes and commas!

Let's explore how FastCSV manages whitespaces when reading CSV files:

```
"value 1","value 2" , "value 3"CRLF
```

In this case, we observe:

1. A proper quoted field `"value 1"`
2. A quoted field with a trailing whitespace `"value 2"_` (the underscore represents the whitespace)

   By default, FastCSV throws a `CsvParseException` when reading this field.
   This behavior can be changed by setting `CsvReaderBuilder.allowExtraCharsAfterClosingQuote(boolean)` to `true`.
   In this case, FastCSV concatenates any trailing characters (including whitespaces) to the field.
   The Java value for this field would be `"value 2 "` (**without** the quotes).

3. A quoted field with a leading whitespace `_"value 3"` (the underscore represents the whitespace)

   FastCSV would handle this field as an unquoted field. Consequently, the Java value for this field would
   be `" value 3"` (**including** the quotes). This decision was made for a couple of reasons:

    - As "Spaces are considered part of a field and SHOULD NOT be ignored" (see "Spaces within fields" above), the
      quotes actually don't enclose the field. The quotes are now part of the field.
    - Some CSV implementations don't use quotes (as a field enclosure) at all. It's not "safe" to assume that a late
      quote character is really a field enclosure. Both formats ("not using quotes when there's a need" and "whitespaces
      between double quotes and commas") are broken per RFC. A choice must be made.
    - For performance reasons, FastCSV switches to unquoted-field-parsing when the first character is not a quote
      character. It's not reasonable to slow down the parser in order to guess which broken format is used.

### Other field enclosures

The RFC does not mention any field enclosures other than double quotes.

FastCSV supports any character as field enclosures when reading or writing CSV files.

### Other field separators

The RFC says:
> This document defines a comma as a field separator but implementers should be aware that some applications may use
> different values

FastCSV supports any character as field separator when reading or writing CSV files.

### Escaping double quotes

The RFC says:
> This document prescribes that a double quote appearing inside a field must be escaped by preceding it with another
> double quote. Implementers should be aware that some applications may choose to use a different escaping mechanism.

FastCSV does not support any other escaping mechanism than escaping double quotes with another double quote.

### BOM header

The RFC says:
> Some applications might be able to read and properly interpret such a header, others could break.

Reading CSV files with a BOM header is supported when enabling BOM header detection
via `CsvReaderBuilder.detectBomHeader(true)`.

FastCSV does not offer an option to write a BOM header. This is mostly because of the now dominant UTF-8 encoding. UTF-8
does not require a BOM header. Let me know if you have a good reason to add this feature.

### Bidirectional text

FastCSV does not contain any special handling for bidirectional text.

### Comments

The RFC says:
> Some implementations may use the hash sign ("#") to mark lines that are meant to be commented line.

FastCSV implements this feature as follows:

When **writing** CSV files: The `CsvWriter.writeComment(String)` method can be used to write a comment line. If
the `String` parameter contains line breaks, FastCSV will automatically prepend a comment character to each line. A call
to `writeComment("foo\nbar")` would result in the following output:

```
#fooCRLF
#barCRLF
```

When writing a regular CSV record, FastCSV will automatically enclose the first field in double quotes if it starts
with the configured comment character. A call to `writeRecord("#foo", "#bar")` would result in the following output:

```
"#foo",#barCRLF
```

When **reading** CSV files: As the RFC does not clearly specify how to handle comments, FastCSV does enable comment
handling by default. The `CsvReaderBuilder.commentStrategy()` method is used to configure the wanted behavior. The
following options are available:

- `CommentStrategies.NONE`: Disable comment handling, read the line as a regular CSV record (default)
- `CommentStrategies.READ`: Parse comment lines as records. The `CsvRecord.isComment()` method can be used to check if
  the record is a comment. The `CsvRecord.getField(0)` method can be used to get the comment text.
- `CommentStrategies.SKIP`: Parse comment lines but skip them; no `CsvRecord` is created/returned for comment lines.

Both the `CsvReaderBuilder` and `CsvWriterBuilder`, allows to configure the character to be used as the comment
character (via `commentCharacter(char)`). The default value is `#`.

When comment handling is enabled, FastCSV handles any line that starts with the configured comment character and that is
not a continuation of a multi-line field as a comment line. The following example illustrates this:

```
# this is a commentCRLF
"# this is not a comment"CRLF
this is also # not a commentCRLF
"this is a multi-lineCRLF
# and therfore not a comment"CRLF
```

### Different end of line characters

While RFC 4180 mentions only the CRLF sequence as the end-of-line character, 4180-bis explicitly includes the CR and LF
characters, while maintaining the CRLF sequence as the default.

In line with the RFC, FastCSV defaults to using the CRLF sequence as the end-of-line character. However, it also
provides support for the CR and LF characters (via `CsvWriterBuilder.lineDelimiter(LineDelimiter)`) when writing CSV
files. The CsvReader, on the other hand, automatically detects the end-of-line character when reading CSV files.

## ABNF Grammar

When comment handling is disabled (default), FastCSV adheres to the ABNF grammar of RFC 4180-bis.

```abnf
file = [header] *(record)

header = [field] *(COMMA field) linebreak

record = [field] *(COMMA field) linebreak

field = (escaped / non-escaped)

escaped = DQUOTE *(textdata / COMMA / CR / LF / 2DQUOTE) DQUOTE

non-escaped = *(textdata)

textdata = %x00-09 / %x0B-0C / %x0E-21 / %x23-2B / %x2D-7F / UTF8-data
         ; all characters except LF, CR, DQUOTE and COMMA

linebreak = CR / LF / CRLF

COMMA = %x2C

CR = %x0D ; as per section B.1 of [RFC5234]

CRLF = CR LF ; as per section B.1 of [RFC5234]

DQUOTE = %x22 ; as per section B.1 of [RFC5234]

LF = %x0A ; as per section B.1 of [RFC5234]

UTF8-data = UTF8-2 / UTF8-3 / UTF8-4 ; as per section 4 of [RFC3629]
```

When comment handling is enabled, FastCSV adheres to the following ABNF grammar:

```abnf
file = *((comment / record) linebreak)

comment = HASH *comment-data

record = first-field *(COMMA field)

linebreak = CR / LF / CRLF

first-field = (escaped / first-non-escaped)

field = (escaped / non-escaped)

escaped = DQUOTE *(data-with-hash / COMMA / CR / LF / 2DQUOTE) DQUOTE

first-non-escaped = [data *data-with-hash]

non-escaped = *data-with-hash

comment-data = %x00-09 / %x0B-0C / %x0E-7F / UTF8-data
         ; all characters except LF, CR

data = %x00-09 / %x0B-0C / %x0E-21 / %x24-2B / %x2D-7F / UTF8-data
         ; all characters except LF, CR, DQUOTE, HASH and COMMA

data-with-hash = data / HASH

HASH = %x23

COMMA = %x2C

CR = %x0D ; as per section B.1 of [RFC5234]

CRLF = CR LF ; as per section B.1 of [RFC5234]

DQUOTE = %x22 ; as per section B.1 of [RFC5234]

LF = %x0A ; as per section B.1 of [RFC5234]

UTF8-data = UTF8-2 / UTF8-3 / UTF8-4 ; as per section 4 of [RFC3629]
```
