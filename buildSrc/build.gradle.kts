plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotbugs)
}

// Prevent build warning: "Kotlin does not yet support 23 JDK target, falling back to Kotlin JVM_22 JVM target"
kotlin {
    jvmToolchain(21)
}
