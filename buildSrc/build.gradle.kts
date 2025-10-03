plugins {
    `kotlin-dsl`
}

kotlin {
    // Kotlin does not yet support 25 JDK target,
    // OSS-Fuzz requires JDK 17
    jvmToolchain(17)
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotbugs)
}
