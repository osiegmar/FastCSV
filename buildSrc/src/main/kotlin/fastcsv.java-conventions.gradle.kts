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

checkstyle {
    toolVersion = "14.4.0"
}

pmd {
    toolVersion = "7.29.0"
    isConsoleOutput = true
    ruleSets = emptyList()
    ruleSetFiles = files("${project.rootDir}/config/pmd/config.xml")
}

// PMD's type resolution otherwise falls back to the JDK running Gradle (25) and judges the code
// against APIs Java 17 doesn't have; PMD 7.27+ warns about that fallback ("Adding current platform").
dependencies {
    pmdAux(javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }
        .map { it.metadata.installationPath.files("lib/jrt-fs.jar") })
}
