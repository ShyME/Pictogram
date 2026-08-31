// The whitelisted Modulith shared module. Carries the ID value types plus the HTTP edge
// conventions every module's web layer reuses (Problem Details, the pagination envelope,
// current-user resolution) — see docs/adr/0008. The web dependencies are `api` so a
// module's `internal.web` package compiles against them; none of them pull a servlet
// container (that stays in :app).
plugins {
    id("pictogram.java-library-conventions")
}

dependencies {
    api("org.springframework:spring-webmvc")
    api("org.springframework.security:spring-security-web")
    api("org.springframework.security:spring-security-oauth2-resource-server")
    api("org.springframework.security:spring-security-oauth2-jose")
    api("tools.jackson.core:jackson-databind")
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Swagger annotations so every module's `internal.web` layer can document its real
    // status codes and Problem Detail responses; springdoc itself only runs in :app, which
    // reads these off the controllers. Version tracks the swagger-core the springdoc starter
    // in :app pulls (springdoc 3.1.0 -> swagger 2.2.52).
    api("io.swagger.core.v3:swagger-annotations-jakarta:2.2.52")

    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework.modulith:spring-modulith-core")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-test")
}
