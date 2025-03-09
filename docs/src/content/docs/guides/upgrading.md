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

## Changed implementation of `CsvIndex` and `CsvPage` to Java records

The `CsvIndex` and `CsvPage` classes have been changed to Java records. With this change, a few method calls have changed as well.

```diff lang="java"
  CsvIndex csvIndex = yourCodeToBuildTheIndex();

  // Simple get-prefix removal
- csvIndex.getBomHeaderLength();
+ csvIndex.bomHeaderLength();

- csvIndex.getFileSize();
+ csvIndex.fileSize();

- csvIndex.getFieldSeparator();
+ csvIndex.fieldSeparator();

- csvIndex.getQuoteCharacter();
+ csvIndex.quoteCharacter();

- csvIndex.getCommentStrategy();
+ csvIndex.commentStrategy();

- csvIndex.getCommentCharacter();
+ csvIndex.commentCharacter();

- csvIndex.getRecordCount();
+ csvIndex.recordCount();

  // Replaced getPageCount() with pages().size()
- csvIndex.getPageCount();
+ csvIndex.pages().size();

  // Replaced getPage(int) with direct access to the pages list
- var firstPage = csvIndex.getPage(0);
+ var firstPage = csvIndex.pages().getFirst();

  // Again, simple get-prefix removal
- firstPage.getOffset();
+ firstPage.offset()

- firstPage.getStartingLineNumber();
+ firstPage.startingLineNumber();
```
