---
title: FAQ
description: Frequently asked questions about FastCSV.
tableOfContents:
  minHeadingLevel: 2
  maxHeadingLevel: 4
---

## General questions

### How to contribute?

Contributions are welcome! Please refer to
the [contribution guidelines](https://github.com/osiegmar/FastCSV/blob/main/.github/CONTRIBUTING.md).

### Where to ask questions? Where to get help?

If you have questions about FastCSV, please ask them
on [GitHub Discussions](https://github.com/osiegmar/FastCSV/discussions).

### How to suggest a feature?

Feature requests can be submitted via the [GitHub issue tracker](https://github.com/osiegmar/FastCSV/issues/new/choose).
Make sure that the feature would align with the [goals of FastCSV](/architecture/goals/).

### How to report a bug?

If you encounter a bug, please report it by creating a new issue on
the [GitHub issue tracker](https://github.com/osiegmar/FastCSV/issues/new/choose). Make sure to include a JUnit test
that reproduces the issue.
This helps to understand the problem and to verify the fix.

Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.

### How to report a security vulnerability?

If you discover a security vulnerability, please report it via https://github.com/osiegmar/FastCSV/security.

### Do I need a CSV library?

Java does not provide built-in support for reading and writing CSV files.

You may wonder why you need a CSV library at all, since CSV files are just text files separated by commas – right?
Well, not so fast! 😉

Think about how to handle:

- enclosed and non-enclosed fields
- fields containing commas, line breaks or (escaped) quotes
- different line endings
- empty lines or empty fields
- optional headers
- BOM (Byte Order Mark) headers
- large files/fields without running out of memory
- garbled data (e.g. data outside of quotes)
- comments

FastCSV handles all these cases and more, so you don't have to worry about them. With only a few kilobytes in size
and zero dependencies, FastCSV is a great choice for reading and writing CSV files in Java.

## Feature related questions

While most parts of this documentation are focused on the features of FastCSV, this section explicitly addresses some
questions about features that are not supported by FastCSV or how to achieve certain tasks.

:::note
Implementing these features would likely conflict with the [goals of FastCSV](/architecture/goals/).
Hence, they are unlikely to be implemented in the future.
:::

### Encoding

#### Does FastCSV support mixed encodings?

FastCSV supports reading and writing CSV files in any encoding supported by Java.

However, it does not support mixed encodings within a single CSV file. The encoding of the file must be consistent.

### Control characters

#### Does FastCSV support field separators with multiple characters?

While CSV stands for "Comma-Separated Values" and the RFC specifies only the comma as the field separator,
FastCSV allows configuring the field separator to support real-world CSV files that may use different separators
like semicolon or tab.

Although it does not support field separators with **multiple** characters (each).

#### Does FastCSV support mixed field separators?

The use of mixed field separators (e.g., comma **and** semicolon) within a single CSV file is not supported by FastCSV.

The configured field separator (comma by default) is used for the entire CSV file.

#### Does FastCSV support field encapsulation with multiple characters?

While the RFC specifies only *double quotes* to encapsulate fields, FastCSV allows configuring the character
to support real-world CSV files that may use different characters, like single quotes.

However, it does not support field encapsulation with **multiple** characters (each).

#### Does FastCSV support mixed field encapsulation?

The use of mixed field encapsulation (e.g., double quotes **and** single quotes) within a single CSV file is not
supported by FastCSV.

The configured quote character (double quote by default) is used for the entire CSV file.

#### Does FastCSV allow configuring the escape character?

The RFC defines that double quotes are escaped by doubling them.
A string `a " b` would be written as `"a "" b"`.

If you configure FastCSV to use a different quote character, it will also be escaped by doubling it.
A string `a ' b` would be written as `'a '' b'`, if you configure the quote character to be a single quote.

FastCSV does not support configuring a different escape character.

See also: https://github.com/osiegmar/FastCSV/issues/103

### Auto conversion/mapping

#### Does FastCSV support automatic bean mapping?

The goals of FastCSV are not going hand in hand with automatic bean mapping as it would increase the complexity and
footprint of the library and decrease its performance.

If FastCSV otherwise suits your needs, you can easily map CSV records to beans using the Java stream API. 

```java
public class Test {

    public static void main(String[] args) throws IOException {
        var file = Paths.get("input.csv");
        try (var csv = CsvReader.builder().ofNamedCsvRecord(file)) {
            csv.stream()
                .map(Test::mapPerson)
                .forEach(System.out::println);
        }
    }

    private static Person mapPerson(NamedCsvRecord rec) {
        return new Person(
            Long.parseLong(rec.getField("ID")),
            rec.getField("firstName"),
            rec.getField("lastName")
        );
    }

    private record Person(Long id, String firstName, String lastName) {
    }

}
```

#### Does FastCSV support automatic type conversion?

The CSV format does not specify data types. FastCSV reads and writes all fields as strings.

When writing CSV records, you have to convert fields to strings yourself.

When reading CSV records, you have the following options:

- Use the `CsvRecord` API to access fields as strings and convert them to the desired type. You may also combine this
  with the Java stream API (see above).
- Implement a custom `CsvCallbackHandler` to access raw data (`char[]`). Checkout the
  [Reader with custom CallBackHandler example](https://github.com/osiegmar/FastCSV/blob/main/example/src/main/java/example/ExampleCsvReaderWithCustomCallbackHandler.java).
