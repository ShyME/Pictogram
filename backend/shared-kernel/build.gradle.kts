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

    api("io.swagger.core.v3:swagger-annotations-jakarta:2.2.54")

    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework.modulith:spring-modulith-core")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-test")
}
