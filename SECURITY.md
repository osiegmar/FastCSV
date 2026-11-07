# Security Policy

## Supported Versions

Only the 4.x line is maintained. Older lines (3.x, 2.x, 1.x) have reached end of life and receive no
security fixes — please upgrade to 4.x, as no backports are provided.

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

Preferably, [**report it privately via GitHub**](https://github.com/osiegmar/FastCSV/security/advisories/new).
This keeps the report confidential, lets us discuss it in one place and produces an advisory that notifies
downstream users once a fix is released. If you are not sure, please email the maintainer:

oliver[@]siegmar.de

Please include as much of the information listed below as you can to help to better understand and resolve the issue:

* The type of issue (e.g., parser desynchronization, resource exhaustion, or unsafe file handling)
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

From version 4.4.1 on, each release also has a build provenance attestation. It shows that the artifact was built by the release workflow of this repository on GitHub Actions. To verify a downloaded artifact with the GitHub CLI:

    gh attestation verify fastcsv-<version>.jar --repo osiegmar/FastCSV \
        --signer-workflow osiegmar/FastCSV/.github/workflows/release.yml
