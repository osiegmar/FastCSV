plugins {
    java
    checkstyle
    pmd
    id("com.github.spotbugs")
}

repositories {
    mavenCentral()
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter = file("${project.rootDir}/config/spotbugs/config.xml")
    reports.maybeCreate("xml").required = false
    reports.maybeCreate("html").required = true
}

pmd {
    // Version bundled with Gradle 8.10 is not able to run on Java 23.
    toolVersion = "7.5.0"
    isConsoleOutput = true
    ruleSets = emptyList()
    ruleSetFiles = files("${project.rootDir}/config/pmd/config.xml")
}
