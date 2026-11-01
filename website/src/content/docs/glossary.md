---
title: CSV Glossary
description: "CSV terms explained for Java developers – record, field, header, separator, quoting, empty field vs. null, comments, BOM – and how FastCSV handles them."
---

CSV has no single vocabulary. Depending on who you ask, the same thing is called a *row*, a *line* or a *record*;
a *value*, a *cell* or a *field*; a *delimiter* or a *field separator*; an *enclosure*,
a *text qualifier* or a *quote character*.
This glossary explains the terms used throughout the FastCSV documentation and Javadoc and shows where FastCSV
handles them. Under *RFC 4180*, each term notes how FastCSV relates to the RFC: *as specified*, *stricter* (rejects
CSV data RFC 4180 permits), *tolerated* (accepts CSV data RFC 4180 does not permit), *deviates* (reads CSV data RFC 4180
permits differently) or an opt-in extension. Under *Known elsewhere as*, it lists names used by other tools and documents. This documentation uses
the term in the heading; some API names predate this glossary (for example `lineDelimiter` for the line break or
`FieldMismatchStrategy.SKIP` for dropping a record).

## Structure

### CSV data

The complete text being read or written – from a file, a stream or a string.

**RFC 4180:** Any character set is allowed, as specified (via the `charset` parameter); FastCSV defaults to UTF-8. Tolerated: RFC 4180's grammar restricts field content to printable ASCII; FastCSV accepts any character, because real-world CSV data contains non-ASCII text and control characters such as tabs. [Details](/architecture/interpretation/#encoding)

### Record

One entry of the CSV data: an ordered list of fields.
A record usually occupies one line but can span several lines when a quoted field contains a line break.
In FastCSV, a record is a `CsvRecord` (fields by index) or a `NamedCsvRecord` (fields by field name).

**RFC 4180:** As specified.

*Known elsewhere as:* row, line

### Line

A sequence of characters ended by a line break or by the end of the CSV data.
Because one record can span several lines, line and record are not the same thing.

### Field

One item of a record.
`CsvRecord.getField(int)` returns a field by its field index, `NamedCsvRecord.getField(String)` by its field name.

**RFC 4180:** As specified.

*Known elsewhere as:* value, cell, column

### Empty field

A field without content, either written as nothing (`a,,c`) or as an empty quoted field (`a,"",c`).
CSV has no concept of `null`: FastCSV returns an empty field as an empty string, never as `null`.
To read empty fields as `null`, see [Reading Empty Fields as null](/guides/examples/reading-null/).

**RFC 4180:** As specified – RFC 4180 has no concept of `null` either.

*Known elsewhere as:* null value, missing value, blank field

### Header

The first record, when it contains field names instead of data.
Read it with `CsvReader.builder().ofNamedCsvRecord(…)`.
See also [Header validation](/guides/examples/header-validation/).

**RFC 4180:** As specified; the header is optional. Stricter: duplicate field names are rejected by default to avoid ambiguous access by name. [Details](/architecture/interpretation/#unique-field-names)

*Known elsewhere as:* header row, column names

### Field name

A field of the header, used to access fields by name.

*Known elsewhere as:* column name, header name

### Field count

The number of fields in a record (`CsvRecord.getFieldCount()`).
RFC 4180 expects every record to have the same field count.

*Known elsewhere as:* column count

### Extra field / missing field

A record with more (extra field) or fewer (missing field) fields than the first record.
By default, FastCSV rejects such records with a `CsvParseException`.
Use `extraFieldStrategy` / `missingFieldStrategy` to accept them as they are or to drop them.
See [Reading ambiguous data](/guides/examples/reading-ambiguous-data/).

**RFC 4180:** Stricter: RFC 4180 only says that records *should* have the same field count; FastCSV rejects mismatches by default instead of guessing which field belongs where. [Details](/architecture/interpretation/#different-field-count)

*Known elsewhere as:* ragged rows, jagged rows, inconsistent column count

### Empty line

A line without any characters.
Readers disagree on whether it is a record with one empty field or no record at all.
FastCSV skips empty lines by default (`skipEmptyLines`).
If not skipped, an empty line is read as a record with one empty field, but it never serves as the header and is exempt from the field count check.

**RFC 4180:** Deviates: under RFC 4180, an empty line is a record with one empty field. Skipping is the default for compatibility. [Details](/architecture/interpretation/#empty-lines)

*Known elsewhere as:* blank line, empty row

## Positions

### Line number

The 1-based position of a line, as shown by a text editor.
`CsvRecord.getStartingLineNumber()` returns the line number of the first line of a record – even if earlier records
spanned several lines or lines were skipped – which makes it useful for error messages.

### Field index

The 0-based position of a field within its record, as used by `CsvRecord.getField(int)`.

*Known elsewhere as:* column index

## Delimiting characters

### Field separator

The character between two fields of a record – a comma by default.
FastCSV also supports other characters (e.g. `;` or tab) and, when reading, multi-character separators.
See [Non-standard control characters](/guides/examples/non-standard-control-characters/).

**RFC 4180:** RFC 4180 defines only the comma; other separators are an opt-in extension.

*Known elsewhere as:* delimiter, comma

### Line break

CR, LF or CRLF.
Outside a quoted field, a line break ends a record; inside a quoted field, it is part of the field content.
When reading, FastCSV recognizes all three, even mixed within the same CSV data.
When writing, it uses CRLF as required by RFC 4180, unless configured otherwise (`lineDelimiter`).

**RFC 4180:** Writing uses CRLF as specified. Tolerated when reading: RFC 4180 defines only CRLF, but CR and LF are common in practice, e.g. in files created by Unix tools. [Details](/architecture/interpretation/#cr-and-lf-line-breaks)

*Known elsewhere as:* newline, line ending, end-of-line (EOL), line terminator, line delimiter

### Dialect

The combination of field separator, quote character, comment character and line break that CSV data is written in.
FastCSV does not guess the dialect; you configure it on the builder.

**RFC 4180:** RFC 4180 defines a single dialect – comma, double quote, CRLF, no comments – which is FastCSV's default for writing. When reading, FastCSV also accepts CR and LF line breaks.

*Known elsewhere as:* CSV format, CSV flavor

## Quoting

### Quote character

The character that encloses a quoted field – a double quote (`"`) by default.

**RFC 4180:** RFC 4180 defines only the double quote; other quote characters are an opt-in extension.

*Known elsewhere as:* enclosure, text qualifier

### Quoted field

A field enclosed in quote characters.
Inside a quoted field, field separators and line breaks are part of the field instead of ending it.

**RFC 4180:** As specified. Tolerated when reading: a quote character inside an unquoted field (`ab"cd`) is read as part of the field, although RFC 4180 does not allow it. [Details](/architecture/interpretation/#abnf-grammar)

*Known elsewhere as:* enclosed field, escaped field

### Escape pair

Two consecutive quote characters inside a quoted field, standing for one quote character in the field:
`"say ""hello"""` is read as `say "hello"`.
FastCSV handles this automatically, both when reading and writing.

**RFC 4180:** As specified.

*Known elsewhere as:* escaped quote, doubled quote

### Quoting

Writing a field as a quoted field.
It is required when the field contains the field separator, the quote character or a line break, or when the first
field of a record begins with the comment character; otherwise it is optional.
`CsvWriter` quotes only when required by default; a `QuoteStrategy` lets you quote more often.
See [Quote strategies](/guides/examples/quote-strategies/).

**RFC 4180:** As specified. Quoting a first field that begins with the comment character is permitted, as RFC 4180 makes quoting optional; it keeps such records from being read as comments.

*Known elsewhere as:* escaping, enclosing

## Comments

### Comment

A line that starts with the comment character – `#` by default – where a record would begin.
A line inside a multi-line quoted field is therefore never a comment, even if it starts with `#`.
Comments are not part of RFC 4180 but widely used.
A comment is not a record; with `commentStrategy` you choose whether FastCSV treats such lines as records (the default),
skips them or returns them as comments.
See [Comments in CSV Files](/guides/examples/handle-comments/).

**RFC 4180:** Not part of RFC 4180. Comment handling is off by default, so such lines are read as regular records, as RFC 4180 requires.

*Known elsewhere as:* comment line

## Malformed CSV data

### Malformed CSV data

CSV data that violates RFC 4180 beyond the variations FastCSV supports (other dialects, any character in fields,
comments, CR or LF line breaks, a byte order mark).

**RFC 4180:** RFC 4180 does not define how to handle malformed CSV data.

*Known elsewhere as:* invalid CSV, broken CSV

### Rejection and tolerance

When CSV data is malformed or ambiguous, FastCSV fails with a `CsvParseException` by default (rejection) instead of
guessing.
Specific cases can be accepted on request (tolerance), e.g. with `trimWhitespacesAroundQuotes`; a few are tolerated
by default, such as an unclosed quoted field.
See [How FastCSV Interprets RFC 4180](/architecture/interpretation/) for the details.

**RFC 4180:** Not covered by RFC 4180. Rejecting by default is FastCSV's choice, so that malformed CSV data is not misread.

*Known elsewhere as:* strict and lenient parsing

### Unclosed quoted field

A quoted field without a closing quote character before the CSV data ends.
FastCSV 4.x currently tolerates it by default and returns the rest of the CSV data as the field; use
`allowUnclosedQuote(false)` to reject it. The default changes to rejection in FastCSV 5.0.

**RFC 4180:** Tolerated: RFC 4180 does not allow it. FastCSV 4.x tolerates it by default for compatibility; FastCSV 5.0 rejects it. [Details](/architecture/interpretation/#unclosed-quoted-fields)

*Known elsewhere as:* unclosed quote, unbalanced quotes, premature EOF, unexpected end of input

## Whitespace and encoding

### Trim

Removing leading and trailing whitespace from a field.
FastCSV does not trim fields by default, because spaces are part of a field according to RFC 4180.
Use `FieldModifiers.TRIM` (or `STRIP` for Unicode whitespace) – see [Modifying input](/guides/examples/modifying-input/).

**RFC 4180:** As specified: spaces are part of a field, so trimming is opt-in. [Details](/architecture/interpretation/#spaces-within-fields)

*Known elsewhere as:* strip

### Byte order mark (BOM)

The invisible character U+FEFF at the very beginning of a file, written by some tools (e.g. Excel) to mark UTF-8,
UTF-16 or UTF-32 encoding.
When reading UTF-8 without detection, it ends up at the start of the first field – with a header, the first field name.
Enable detection with `detectBomHeader(true)`; it applies when reading from a `Path` or `InputStream`, not from a
`Reader` or `String`. See [Byte Order Mark (BOM) in CSV Files](/guides/examples/byte-order-mark/).

**RFC 4180:** Not part of RFC 4180; detection is opt-in. [Details](/architecture/interpretation/#byte-order-mark-bom)

*Known elsewhere as:* BOM header, BOM character
