plugins {
    `kotlin-dsl`
}

kotlin {
    // Kotlin does not yet support 25 JDK target
    jvmToolchain(24)
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotbugs)
}
