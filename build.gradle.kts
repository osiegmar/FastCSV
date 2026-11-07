plugins {
    base
    alias(libs.plugins.jreleaser)
}

group = "de.siegmar"
version = "4.5.0-SNAPSHOT" // x-release-please-version

subprojects {
    group = rootProject.group
    version = rootProject.version
}

jreleaser {
    project {
        gitRootSearch.set(true)
        name.set("FastCSV")
        description.set("Fast, lightweight, RFC 4180 compliant CSV library for Java with zero dependencies.")
        authors.set(listOf("Oliver Siegmar"))
        license.set("MIT")
        links {
            homepage.set("https://fastcsv.org")
            license.set("https://opensource.org/licenses/MIT")
        }
    }
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        pgp {
            armored.set(true)
        }
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    // sign, checksum and verify; JReleaser documents another default than it applies
                    applyMavenCentralRules.set(true)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepositories.add("lib/build/staging-deploy")
                }
            }
        }
    }
}
