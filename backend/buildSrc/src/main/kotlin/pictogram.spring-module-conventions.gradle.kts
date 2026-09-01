plugins {
    id("pictogram.java-library-conventions")
}

dependencies {
    api(project(":shared-kernel"))

    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    testImplementation(project(":test-support"))
}
