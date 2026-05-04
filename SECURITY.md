# Security Policy

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

Preferably, use the **GitHub Security Advisory** feature. If you are not sure, please email the maintainer:

oliver[@]siegmar.de

Please include as much of the information listed below as you can to help to better understand and resolve the issue:

* The type of issue (e.g., buffer overflow, SQL injection, or cross-site scripting)
* Full paths of source file(s) related to the manifestation of the issue
* The location of the affected source code (tag/branch/commit or direct URL)
* Any special configuration required to reproduce the issue
* Step-by-step instructions to reproduce the issue
* Proof-of-concept or exploit code (if possible)
* Impact of the issue, including how an attacker might exploit the issue

This information will help to triage your report more quickly.

## Verifying Release Artifacts

FastCSV releases on Maven Central are signed with the maintainer's OpenPGP key. The fingerprint of the key currently used to sign releases is:

    206D F7C4 8E3D 6CB1 45DD  A835 FF84 4A32 56C7 FB01

The full public key, together with verification instructions, is published in the [`KEYS`](KEYS) file at the root of this repository. To verify a downloaded artifact:

    gpg --import KEYS
    gpg --verify fastcsv-<version>.jar.asc fastcsv-<version>.jar

Consumers using [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html) can pin the above fingerprint as a trusted key for `de.siegmar:fastcsv`.
