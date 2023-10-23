@file:Suppress("StringLiteralDuplication")

plugins {
    id("fastcsv.java-conventions")
    `java-library`
    `maven-publish`
    signing
    jacoco
    id("me.champeau.jmh") version "0.7.1"
    id("info.solidsoft.pitest") version "1.9.11"
    id("ru.vyarus.animalsniffer") version "1.7.1"
    id("biz.aQute.bnd.builder") version "6.4.0"
}

group = "de.siegmar"
version = "3.0.0-SNAPSHOT"

project.base.archivesName = "fastcsv"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.javadoc {
    javadocTool.set(javaToolchains.javadocToolFor {
        languageVersion.set(JavaLanguageVersion.of(20))
    })
    options.jFlags = listOf("-Duser.language=en")
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
    commonImplementation("org.assertj:assertj-core:3.24.2")

    testImplementation(platform("org.junit:junit-bom:5.9.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    "intTestImplementation"(project)

    signature("com.toasttab.android:gummy-bears-api-33:0.5.1@signature")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

val intTestTask = tasks.register<Test>("intTest") {
    group = "verification"
    useJUnitPlatform()

    testClassesDirs = intTest.output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath

    finalizedBy(tasks.jacocoTestReport)
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(intTestTask)
}

pitest {
    junit5PluginVersion = "1.2.0"
    targetClasses = setOf("blackbox.*", "de.siegmar.*")
    timestampedReports = false
}

tasks.jacocoTestReport {
    executionData = files(fileTree(layout.buildDirectory).include("/jacoco/*.exec"))
    reports {
        xml.required.set(true)
    }
    dependsOn(intTestTask)
}

tasks.jacocoTestCoverageVerification {
    executionData = files(fileTree(layout.buildDirectory).include("/jacoco/*.exec"))
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
    dependsOn(intTestTask)
}

tasks.jmh {
    warmupIterations = 2
    iterations = 5
    benchmarkMode = listOf("thrpt")
    fork = 2
    operationsPerInvocation = 1
}

tasks.jar {
    manifest {
        attributes("Bundle-SymbolicName" to "de.siegmar.fastcsv",
                "-exportcontents" to "!de.siegmar.fastcsv.util, *",
                "-removeheaders" to "Private-Package")
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
                description = "Ultra fast and simple RFC 4180 compliant CSV library."
                url = "https://github.com/osiegmar/FastCSV"
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
            name = "ossrh"
            credentials(PasswordCredentials::class)
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            } else {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
