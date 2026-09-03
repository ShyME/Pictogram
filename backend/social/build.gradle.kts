plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    implementation(project(":post"))

    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
}
