---
title: How FastCSV Interprets RFC 4180
description: "How FastCSV reads RFC 4180 CSV: quoted fields, line breaks in fields, escaped double quotes, empty fields, field count, and how malformed CSV is handled."
sidebar:
  label: CSV Interpretation
---

At first, the CSV format may seem very simple. In practice, however, numerous edge cases need to be considered.
The [JavaCsvComparison](https://github.com/osiegmar/JavaCsvComparison) project illustrates that there are many different
ways to interpret CSV files, especially when not strictly following the RFC specification. Some of these differences
might be considered as bugs, while others represent alternative approaches to handling edge cases (mostly malformed CSV
data).

This document describes how FastCSV interprets CSV data and how this interpretation aligns with the RFC specification.
For the terms used here, see the [CSV glossary](/glossary/).

## Status of the RFC

[RFC 4180](https://datatracker.ietf.org/doc/html/rfc4180) dates back to 2005. Since then, notable changes have occurred.
For instance, it originally described common character set usage as US-ASCII and made no mention of Unicode. Nowadays,
UTF-8 has become the standard for CSV files.

Yakov Shafranovich, the original author of RFC 4180, and I (Oliver Siegmar, the author of FastCSV) conducted a thorough
review and revision of the RFC. Our focus was on addressing numerous edge cases that arise in practical scenarios.

The result, the 4180-bis draft, was submitted to the IETF but has expired without being published as an RFC.
**RFC 4180 – an informational RFC – therefore remains the reference FastCSV is built on.** The last version of the draft,
[draft-shafranovich-rfc4180-bis-07](https://datatracker.ietf.org/doc/html/draft-shafranovich-rfc4180-bis-07), is still available,
as is its GitHub repository [osiegmar/rfc4180-bis](https://github.com/osiegmar/rfc4180-bis). It documents the reasoning
behind several of the decisions described below. Quotations from the draft are taken from this version and marked as such.

## Implementation details

### Encoding

RFC 4180 names US-ASCII as common usage but allows other character sets via the `charset` parameter of the
`text/csv` MIME type. Today, CSV data commonly contains non-ASCII text, and UTF-8 has become the de facto standard.

FastCSV supports any encoding when reading or writing CSV files while defaulting to UTF-8.

The only exception is `IndexedCsvReader`: because it builds its index on byte level, it is limited to UTF-8 and
single-byte encodings. See [Indexed reading](/guides/examples/indexed-read/) for details.

### Empty fields and null

The CSV format itself does not provide a way to distinguish between an empty field and `null`. This can be a problem
when such a distinction is required.

FastCSV provides the following options to handle `null`:

**on Writing**: by default, both `null` and an empty string are written as an unquoted empty field. With
`QuoteStrategies.EMPTY`, an empty string is written as a quoted empty field (`""`) while `null` stays unquoted, so the two
can be told apart. This helps to produce PostgreSQL-compatible CSV files for example. If you instead want to write a
specific string for `null`, you have to pass the string to the `CsvWriter.writeRecord()` method.

**on Reading**: the `CsvCallbackHandler.addField()` method gets passed a parameter `quoted` that indicates whether the
field was quoted or not. The standard record implementation (`CsvRecord`) does currently not provide a way to
distinguish between an unquoted and a quoted empty field by itself but a custom implementation could use the `quoted`
parameter to do so.

Example for **empty** and **quoted empty** fields:

```
1,,fooCRLF
2,"",barCRLF
```

### Line break after the last record

RFC 4180 says:
> The last record in the file may or may not have an ending line break.

This optional line break creates ambiguity, especially when the CSV file consists of only one field per record.
In such cases, it becomes impossible to determine whether the following example has a third record (containing an
empty field) or not:

```
fooCRLF
barCRLF
```

To avoid this ambiguity, FastCSV writes a line break after every record, including the last one – which RFC 4180
permits and the 4180-bis draft proposed to make mandatory.

### Different field count

RFC 4180 says:
> Each line should contain the same number of fields throughout the file.

In practice, however, this is not always the case. This prompts the question of what the correct behavior should be.

Consider the following CSV snippet as an illustration of varying field counts:

```
header_a,header_bCRLF
value_a_1CRLF
value_a_2,value_b_2,value_c_2CRLF
```

In this example, `value_a_1` likely belongs to `header_a`, and the first record after the header has no field for
`header_b`. However, this is just an assumption. Field `value_c_2` does not even have a corresponding field name.

To ensure no misinterpretation, FastCSV does not allow extra or missing fields in a record by default.
This means that the above example would result in a `CsvParseException` when reading it with FastCSV.

However, this behavior can be changed by setting `CsvReaderBuilder.extraFieldStrategy(FieldMismatchStrategy)`
and `CsvReaderBuilder.missingFieldStrategy(FieldMismatchStrategy)` to `IGNORE` or `SKIP`.

### Empty lines

As a consequence of not dictating equal field counts, a line could be completely empty. This is particularly relevant
for a CSV file with only one field per record, raising the question of whether the field is empty or if the line is empty and
should be skipped.

```
value_1CRLF
CRLF
value_2CRLF
```

By default, FastCSV skips empty lines (see `CsvReaderBuilder.skipEmptyLines(boolean)`). Reading such a file would result
in two records (containing `"value_1"` and `"value_2"`).
Strictly, this deviates from RFC 4180, under which an empty line is a record with one empty field. The default is kept
for compatibility.

To regularly read empty lines, you can disable this behavior, resulting in three records (containing `"value_1"`, `""`,
and `"value_2"`).

### Empty files

Empty files (zero bytes) can be derived from the RFC 4180 grammar (as a single record with one empty field).
FastCSV interprets them as containing no records and returns an empty stream.

### Fields spanning multiple lines

Quoted fields can span multiple lines.

```
"a multi-lineCRLF
field"CRLF
```

FastCSV supports this for reading and writing. The field content would be `"a multi-line\r\nfield"`.
Line breaks within a quoted field are preserved.

### Unique field names

The 4180-bis draft notes:
> Implementers should be aware that some applications may treat header values as unique
> (either case-sensitive or case-insensitive).

```
header_a,header_aCRLF
value_1,value_2CRLF
```

The `NamedCsvRecord` class in FastCSV offers several options to handle this scenario:

- By default, FastCSV does **not** allow duplicate field names to prevent misinterpretation of data.
  This behavior can be changed by calling `allowDuplicateHeaderFields(true)` on the `NamedCsvRecordHandlerBuilder`.
- Methods like `getField("header_a")`, `findField("header_a")`, and `getFieldsAsMap()` return only the **first** field content (`"value_1"`).
- Methods like `findFields("header_a")` and `getFieldsAsMapList()` return a list containing **all** field contents (`"value_1"` and `"value_2"`).

Regardless of the option chosen, FastCSV always treats field names as case-sensitive.

### Spaces within fields

RFC 4180 says:
> Spaces are considered part of a field and should not be ignored.

FastCSV adheres to this recommendation by reading and writing fields as they are, including any (leading or trailing)
whitespace.

When reading CSV files, you have the option to configure a `FieldModifier` to trim the field content before it is returned
to the user. The following example demonstrates trimming, printing `"foo"` and `"bar"` without any whitespace:

```java
var handler = CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));
CsvReader.builder().build(handler, " foo , bar ")
    .forEach(System.out::println);
```

### Whitespace outside quoted fields

The 4180-bis draft notes:
> When quoted fields are used, this document does not allow whitespace between double quotes and commas.

RFC 4180's grammar doesn't allow it either, and FastCSV never writes whitespace between double quotes and commas.

Let's explore how FastCSV handles whitespace when reading CSV files:

```
"value 1","value 2" , "value 3"CRLF
```

In this case, we observe:

1. A proper quoted field `"value 1"`
2. A quoted field with a trailing whitespace `"value 2"_` (the underscore represents the whitespace)

   By default, FastCSV throws a `CsvParseException` when reading this field.
   By setting `CsvReaderBuilder.trimWhitespacesAroundQuotes(boolean)` to `true`, FastCSV tolerates and trims
   the whitespace. The field content would then be `value 2` (**without** the quotes and the
   trailing whitespace).

3. A quoted field with a leading whitespace `_"value 3"` (the underscore represents the whitespace)

   FastCSV would handle this field as an unquoted field. Consequently, the field content would
   be ` "value 3"` (**including** the space and the quote characters). This decision was made for a couple of reasons:

    - As "Spaces are considered part of a field and should not be ignored" (see "Spaces within fields" above), the
      quote characters don't enclose the field. They are part of the field content.
    - Some CSV implementations don't use quote characters at all. It's not "safe" to assume that a late
      quote character really begins a quoted field. Both formats ("not quoting when quoting is required" and "whitespace
      between double quotes and commas") are malformed per RFC. A choice must be made.
    - For performance reasons, FastCSV switches to unquoted-field-parsing when the first character is not a quote
      character. It's not reasonable to slow down the parser in order to guess which broken format is used.

   Setting `CsvReaderBuilder.trimWhitespacesAroundQuotes(boolean)` to `true` changes this behavior as well:
   the leading whitespace is trimmed and the field is treated as quoted, resulting in the field content `value 3`
   (**without** the quote characters).

### Unclosed quoted fields

A quoted field may be unintentionally truncated, leaving an opening quote character without a closing quote character before the CSV data ends:

```
"value 1,value 2CRLF
value 3CRLF
```

By default, FastCSV consumes the remaining characters (including any line breaks) as the field content of the final field.
This tolerance is kept for compatibility with earlier versions.

This tolerance can be turned into a rejection by setting `CsvReaderBuilder.allowUnclosedQuote(false)` (also available
on `IndexedCsvReaderBuilder`). FastCSV then throws a `CsvParseException` that references the starting line of the
offending record.

The default of this flag will change to `false` in version 5.0. To preserve the current tolerance across the
upgrade, set it explicitly to `true`.

### Other quote characters

The RFC does not mention any quote character other than the double quote.

FastCSV supports any character as quote character when reading or writing CSV files.

### Other field separators

The 4180-bis draft notes:
> This document defines a comma as a field separator but implementers should be aware that some applications may use
> different values

FastCSV supports any character as field separator when reading or writing CSV files.

### Escape pairs

The 4180-bis draft notes:
> This document prescribes that a double quote appearing inside a field must be escaped by preceding it with another
> double quote. Implementers should be aware that some applications may choose to use a different escaping mechanism.

FastCSV does not support any other escaping mechanism than escape pairs: a quote character inside a quoted field is
written as two quote characters.

### Byte order mark (BOM)

The 4180-bis draft notes:
> Some applications might be able to read and properly interpret such a header, others could break.

Reading CSV files with a BOM is supported when enabling BOM detection
via `CsvReaderBuilder.detectBomHeader(true)`.

FastCSV does not offer an option to write a BOM. This is mostly because of the now dominant UTF-8 encoding. UTF-8
does not require a BOM. Let me know if you have a good reason to add this feature.

### Bidirectional text

FastCSV does not contain any special handling for bidirectional text.

### Comments

The 4180-bis draft notes:
> Some implementations may use the hash sign ("#") to mark lines that are meant to be commented line.

FastCSV implements this feature as follows:

When **writing** CSV files: The `CsvWriter.writeComment(String)` method can be used to write a comment. If
the `String` parameter contains line breaks, FastCSV writes a separate comment for each line. A call
to `writeComment("foo\nbar")` would result in the following output:

```
#fooCRLF
#barCRLF
```

When writing a regular CSV record, FastCSV automatically quotes the first field if it begins
with the configured comment character. A call to `writeRecord("#foo", "#bar")` would result in the following output:

```
"#foo",#barCRLF
```

When **reading** CSV files: As comments are not part of RFC 4180, FastCSV does not enable comment
handling by default. The `CsvReaderBuilder.commentStrategy()` method is used to configure the wanted behavior. The
following options are available:

- `CommentStrategy.NONE`: Disable comment handling, read the line as a regular CSV record (default)
- `CommentStrategy.READ`: Return comments as `CsvRecord`. The `CsvRecord.isComment()` method can be used to check if
  it holds a comment. The `CsvRecord.getField(0)` method can be used to get the comment text.
- `CommentStrategy.SKIP`: Recognize comments but skip them; no `CsvRecord` is returned for them.

Both `CsvReaderBuilder` and `CsvWriterBuilder` allow configuring the comment character
(via `commentCharacter(char)`). The default is `#`.

When comment handling is enabled, FastCSV reads a comment where a record would begin with the comment character.
A line inside a multi-line quoted field is therefore never a comment. The following example illustrates this:

```
# this is a commentCRLF
"# this is not a comment"CRLF
this is also # not a commentCRLF
"this is a multi-lineCRLF
# and therefore not a comment"CRLF
```

### CR and LF line breaks

RFC 4180 mentions only CRLF as line break. In practice, CSV data with CR or LF line breaks
is common – for example, when created by Unix tools (the 4180-bis draft also proposed accepting CR and LF, while
keeping CRLF as the default).

In line with the RFC, FastCSV writes CRLF by default. However, it also supports writing CR or LF
(via `CsvWriterBuilder.lineDelimiter(LineDelimiter)`). When reading, `CsvReader` recognizes CR, LF and CRLF, even
mixed within the same CSV data.

## ABNF Grammar

When comment handling is disabled (default), the following ABNF grammar, taken from the 4180-bis draft, describes
the CSV data FastCSV reads. Compared to RFC 4180, it allows any character in fields (including control characters
and non-ASCII text) and CR or LF line breaks.
FastCSV's reader has tolerances beyond this grammar in three cases (this also applies to the grammar with comments below):

- The last record may end without a line break, as RFC 4180 permits.
- A quote character inside an unquoted field (`ab"cd`) is read as part of the field.
- An unclosed quoted field is tolerated by default (see [Unclosed quoted fields](#unclosed-quoted-fields)).

```ABNF
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

When comment handling is enabled, the following ABNF grammar describes the CSV data FastCSV reads. The same three
tolerances as above apply; in particular, the last record or comment may end without a line break.

```ABNF
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
