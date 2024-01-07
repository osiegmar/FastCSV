plugins {
    id("fastcsv.java-conventions")
}

dependencies {
    implementation(project(":lib"))
    implementation("ch.randelshofer:fastdoubleparser:1.0.0")
}
