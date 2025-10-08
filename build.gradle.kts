plugins {
    base
    alias(libs.plugins.jreleaser)
}

group = "de.siegmar"
version = "4.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version
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
                    stagingRepositories.add("lib/build/staging-deploy")
                }
            }
        }
    }
    release {
        github {
            changelog {
                formatted.set(org.jreleaser.model.Active.ALWAYS)
                preset.set("conventional-commits")
                append {
                    enabled.set(true)
                    target.set(file("CHANGELOG.md"))
                    title = "## [{{tagName}}] - {{#f_now}}YYYY-MM-dd{{/f_now}}"
                    content = "{{changelogTitle}}\n{{changelogContent}}"
                }
            }
        }
    }
}
