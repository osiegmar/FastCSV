@file:Suppress("StringLiteralDuplication")
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.kotlin.dsl.errorprone

plugins {
    id("fastcsv.java-conventions")
    `java-library`
    `maven-publish`
    jacoco
    alias(libs.plugins.errorprone)
    alias(libs.plugins.jmh)
    alias(libs.plugins.pitest)
    alias(libs.plugins.animalsniffer)
    alias(libs.plugins.bnd)
}

project.base.archivesName = "fastcsv"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    if (name == "compileJmhJava") {
        options.errorprone.isEnabled.set(false)
    } else if (name == "compileTestJava" || name == "compileIntTestJava") {
        options.errorprone.disable("NullAway")
    } else {
        options.errorprone {
            option("NullAway:AnnotatedPackages", "de.siegmar.fastcsv")
        }
    }
}

tasks.compileJava {
    options.release.set(17)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    options.errorprone.error("NullAway")
}

// enable parameter names for tests with @ParameterizedTest
tasks.compileTestJava {
    options.compilerArgs.add("-parameters")
}

tasks.javadoc {
    options.jFlags = listOf("-Duser.language=en", "-Duser.country=US")
}

sourceSets {
    create("common") {
        compileClasspath += sourceSets.main.get().output
    }
    create("intTest") {
        compileClasspath += sourceSets["common"].output
        runtimeClasspath += sourceSets["common"].output
    }
    test {
        compileClasspath += sourceSets["common"].output
        runtimeClasspath += sourceSets["common"].output
    }
}

val commonImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val intTest by sourceSets.getting
configurations[intTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[intTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    errorprone(libs.errorprone)
    errorprone(libs.nullaway)

    commonImplementation(libs.assertj.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    "intTestImplementation"(project)
    "intTestImplementation"(libs.jackson.dataformat.yaml)

    signature(libs.gummy.bears) {
        artifact {
            type = "signature"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

val intTestTask = tasks.register<Test>("intTest") {
    group = "verification"
    useJUnitPlatform()

    testClassesDirs = intTest.output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath

    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(intTestTask, tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}

pitest {
    // Ensure Java 24 compatibility
    pitestVersion = "1.19.5"
    junit5PluginVersion = "1.2.2"
    targetClasses = setOf("blackbox.*", "de.siegmar.*")
    timestampedReports = false
}

tasks.jacocoTestReport {
    executionData = files(fileTree(layout.buildDirectory).include("/jacoco/*.exec"))
    reports {
        xml.required.set(true)
    }
    dependsOn(tasks.test, intTestTask)
}

tasks.jacocoTestCoverageVerification {
    executionData = files(fileTree(layout.buildDirectory).include("/jacoco/*.exec"))
    violationRules {
        rule {
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.9".toBigDecimal()
            }
        }
    }
    dependsOn(tasks.test, intTestTask)
    shouldRunAfter(tasks.jacocoTestReport)
}

tasks.jmh {
    warmupIterations = 2
    iterations = 1
    benchmarkMode = listOf("thrpt")
    fork = 4
    operationsPerInvocation = 1
}

tasks.jar {
    manifest {
        attributes(
            "SPDX-License-Identifier" to "MIT",
            "Bundle-SymbolicName" to "de.siegmar.fastcsv",
            "-exportcontents" to "de.siegmar.fastcsv.reader.*, de.siegmar.fastcsv.writer.*"
        )
    }
    into("META-INF") {
        from(rootDir) {
            include("LICENSE")
        }
    }
}

animalsniffer {
    sourceSets = listOf(project.sourceSets.main.get())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "fastcsv"
            from(components["java"])

            pom {
                name = "FastCSV"
                description = "Lightning-fast, dependency-free CSV library that conforms to RFC standards."
                url = "https://fastcsv.org"
                inceptionYear = "2014"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                scm {
                    url = "https://github.com/osiegmar/FastCSV"
                    connection = "scm:git:https://github.com/osiegmar/FastCSV.git"
                }
                developers {
                    developer {
                        id = "osiegmar"
                        name = "Oliver Siegmar"
                        email = "oliver@siegmar.de"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
