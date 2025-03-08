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

## SimpleFieldModifier

The `SimpleFieldModifier` class has been removed. It was a simple implementation of the `FieldModifier` interface that is now replaced by the `FieldModifiers#transform(Function<String, String>)` method.

Old way:
```java
SimpleFieldModifier normalizeWhitespaces = field -> field.replaceAll("\\s", " ");
```

New way:
```java
FieldModifier normalizeWhitespaces = FieldModifiers.transform(field -> field.replaceAll("\\s", " "));
```

## Upper and lower case field modifiers

The edge-case methods `FieldModifiers#lower(Locale)` and `FieldModifiers#upper(Locale)` have been removed in favor of a more versatile `FieldModifiers#transform(Function<String, String>)` method.

Old way:
```java
FieldModifier toLowerFieldModifier = FieldModifiers.lower(Locale.ENGLISH);
FieldModifier toUpperFieldModifier = FieldModifiers.upper(Locale.ENGLISH);
```

New way:
```java
FieldModifier toLowerFieldModifier = FieldModifiers.transform(field -> field.toLowerCase(Locale.ENGLISH));
FieldModifier toUpperFieldModifier = FieldModifiers.transform(field -> field.toUpperCase(Locale.ENGLISH));
```
