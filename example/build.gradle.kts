plugins {
    id("fastcsv.java-conventions")
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.fastdoubleparser)
}

tasks.checkstyleMain {
    // disable checkstyle for this module as it currently does not support
    // compact Source Files and Instance Main Methods (JEP 512)
    enabled = false
}
