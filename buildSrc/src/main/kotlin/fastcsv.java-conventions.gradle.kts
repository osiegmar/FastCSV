plugins {
    java
    checkstyle
    pmd
    id("com.github.spotbugs")
}

repositories {
    mavenCentral()
}

spotbugs {
    // Version bundled with Gradle is not able to run on Java 24.
    toolVersion = "4.9.3"
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter = file("${project.rootDir}/config/spotbugs/config.xml")
    reports.maybeCreate("xml").required = false
    reports.maybeCreate("html").required = true
}

pmd {
    // Version bundled with Gradle is not able to run on Java 24.
    toolVersion = "7.12.0"
    isConsoleOutput = true
    ruleSets = emptyList()
    ruleSetFiles = files("${project.rootDir}/config/pmd/config.xml")
}
