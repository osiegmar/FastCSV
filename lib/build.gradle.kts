@file:Suppress("StringLiteralDuplication")

plugins {
    id("fastcsv.java-conventions")
    `java-library`
    `maven-publish`
    jacoco
    alias(libs.plugins.jmh)
    alias(libs.plugins.pitest)
    alias(libs.plugins.animalsniffer)
    alias(libs.plugins.bnd)
    alias(libs.plugins.jreleaser)
}

group = "de.siegmar"
version = "3.7.0"

project.base.archivesName = "fastcsv"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.compileJava {
    options.release.set(11)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
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
    junit5PluginVersion = "1.2.1"
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
            "Bundle-SymbolicName" to "de.siegmar.fastcsv",
            "-exportcontents" to "de.siegmar.fastcsv.reader.*, de.siegmar.fastcsv.writer.*"
        )
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

jreleaser {
    project {
        gitRootSearch.set(true)
        name.set("FastCSV")
        description.set("Lightning-fast, dependency-free CSV library that conforms to RFC standards.")
        authors.set(listOf("Oliver Siegmar"))
        license.set("MIT")
        links {
            homepage.set("https://fastcsv.org")
            license.set("https://opensource.org/licenses/MIT")
        }
    }
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepositories.add("build/staging-deploy")
                }
            }
        }
    }
}
