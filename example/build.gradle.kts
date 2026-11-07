plugins {
    id("fastcsv.java-conventions")
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.fastdoubleparser)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // The examples format numbers with the default locale, the expected output must not depend on the machine.
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    systemProperty("updateExpected", providers.gradleProperty("updateExpected").isPresent)
}
