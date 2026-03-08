#!/bin/bash -e

rm -f checksums-1.txt checksums-2.txt

SOURCE_DATE_EPOCH=$(date +%s)
export SOURCE_DATE_EPOCH

function calculate_checksums() {
    OUTPUT=$1

    ./gradlew \
        --no-build-cache \
        -Dorg.gradle.java.installations.auto-download=false \
        clean \
        assemble

    find ./lib/build -name '*.jar' \
        | grep '/build/libs/' \
        | grep --invert-match 'javadoc' \
        | sort \
        | xargs sha256sum > "${OUTPUT}"
}

calculate_checksums checksums-1.txt
calculate_checksums checksums-2.txt

diff checksums-1.txt checksums-2.txt
