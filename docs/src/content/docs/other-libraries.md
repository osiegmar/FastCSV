---
title: Other libraries
description: Other libraries for reading and writing CSV files in Java.
---

While FastCSV offers a great mix of features, performance, and usability, it might not be the right choice for every use
case. Here are some other popular open-source Java libraries you might want to give a try:

## Apache Commons CSV

Under the Apache umbrella, [Commons CSV](https://commons.apache.org/proper/commons-csv/) is a well-known library
for reading and writing CSV files.

**Things to consider:**

- Comes with 3rd party dependencies which can lead to dependency conflicts
- With 923 KiB, including dependencies, it's not a lightweight library
- It [struggles](https://github.com/osiegmar/JavaCsvComparison) with some non-standard CSV files
- It has a quite [low performance](https://github.com/osiegmar/JavaCsvBenchmarkSuite)
- Many [open issues](https://issues.apache.org/jira/browse/CSV)
- No features known that FastCSV doesn't offer

## Jackson CSV

Mostly famous for JSON processing, the Jackson library also offers
a [module](https://github.com/FasterXML/jackson-dataformats-text) for reading and writing CSV files.

**Things to consider:**

- Comes with 3rd party dependencies which can lead to dependency conflicts
- With 2.4 megabytes, including dependencies, it's not a lightweight library
- It [struggles](https://github.com/osiegmar/JavaCsvComparison) with some non-standard CSV files
- While having an active community, there are many [open and stale issues](https://github.com/FasterXML/jackson-dataformats-text/issues)

## Opencsv

Right after Apache Commons CSV, [Opencsv](https://opencsv.sourceforge.net/) is one of the most popular libraries for
reading and writing CSV files in Java.

**Things to consider:**

- Comes with 3rd party dependencies which can lead to dependency conflicts
- With 2.7 megabytes, including dependencies, it's not a lightweight library
- Not fully [standards-compliant](https://github.com/osiegmar/JavaCsvComparison)
- It has a quite [low performance](https://github.com/osiegmar/JavaCsvBenchmarkSuite)
- Unable to handle comments or to skip empty lines (not a standard CSV feature but often required)

## SimpleFlatMapper

Another quite high-performance library is [SimpleFlatMapper](https://simpleflatmapper.org/) that focuses on Bean mapping.

**Things to consider:**

- Comes with 3rd party dependencies which can lead to dependency conflicts
- With 1.5 megabytes, including dependencies, it's not a lightweight library
- Use bytecode manipulation which could lead to compatibility issues
- It [struggles](https://github.com/osiegmar/JavaCsvComparison) with some non-standard CSV files
- While having an active community, there are many [open and stale issues](https://github.com/arnaudroger/SimpleFlatMapper/issues)

## Deprecated libraries

Other, once popular libraries are excluded from this list because they don't seem to be actively maintained anymore:

- [Java CSV](https://sourceforge.net/projects/javacsv/) (last release in 2008)
- [Super CSV](https://super-csv.github.io/super-csv/index.html) (last release in 2015)
- [UniVocity](https://github.com/uniVocity/univocity-parsers) (website down, inactive GitHub repository)

:::note
There are many more libraries available, but this overview focuses on the most popular ones.
:::
