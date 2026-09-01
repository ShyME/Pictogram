plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    // media owns the `media` schema (ADR-0009). The starter brings only flyway-core;
    // Flyway needs the database module on the classpath to recognise Postgres.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // The image bytes live in object storage (MinIO locally, any S3-compatible store in
    // prod), reached over the S3 API. url-connection-client keeps the transport to the JDK
    // HTTP client rather than pulling Apache HttpClient or Netty.
    implementation(platform("software.amazon.awssdk:bom:2.54.7"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")

    // ImageIO codec plugins registered by SPI: a robust JPEG reader (CMYK, broken files)
    // and WebP read support. PNG ships with the JDK. No HEIC — decoding it needs a native
    // libheif, out of scope for v1 (ADR-0006); such uploads fall through to the 400.
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.14.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.14.0")

    // Reads the EXIF orientation tag off an upload so the canonical rendition is upright
    // before the metadata is dropped.
    implementation("com.drewnoakes:metadata-extractor:2.21.0")

    testImplementation(project(":test-support"))

    // Writes EXIF/GPS onto a generated JPEG so a test can assert the pipeline strips it.
    testImplementation("org.apache.commons:commons-imaging:1.0.0-alpha6")
}
