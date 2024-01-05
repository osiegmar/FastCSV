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

// Need to add this as PMD 7 (required for Java21) is not yet released
dependencies {
    pmd("net.sourceforge.pmd:pmd-ant:7.0.0-rc4")
    pmd("net.sourceforge.pmd:pmd-java:7.0.0-rc4")
}

pmd {
    isConsoleOutput = true
    ruleSets = emptyList()
    ruleSetFiles = files("${project.rootDir}/config/pmd/config.xml")
}
