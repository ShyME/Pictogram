plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    implementation(platform(libs.awssdk.bom))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")

    implementation(libs.twelvemonkeys.imageio.jpeg)
    implementation(libs.twelvemonkeys.imageio.webp)

    implementation(libs.metadata.extractor)

    testImplementation(project(":test-support"))

    testImplementation(libs.commons.imaging)
}
