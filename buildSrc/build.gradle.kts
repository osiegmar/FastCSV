plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotbugs)
}

kotlin {
    jvmToolchain(21)
}
