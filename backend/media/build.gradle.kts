plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    implementation(platform("software.amazon.awssdk:bom:2.54.7"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")

    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.14.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.14.0")

    implementation("com.drewnoakes:metadata-extractor:2.21.0")

    testImplementation(project(":test-support"))

    testImplementation("org.apache.commons:commons-imaging:1.0.0-alpha6")
}
