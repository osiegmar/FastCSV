plugins {
    id("fastcsv.java-conventions")
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.fastdoubleparser)
}
