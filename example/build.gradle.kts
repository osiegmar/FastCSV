plugins {
    id("fastcsv.java-conventions")
}

dependencies {
    implementation(project(":lib"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
