plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    implementation(project(":media"))
}
