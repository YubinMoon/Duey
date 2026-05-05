plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.terry.duey"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    runtimeOnly("org.xerial:sqlite-jdbc:3.50.3.0")

    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

tasks.register<org.springframework.boot.gradle.tasks.run.BootRun>("bootRunDebug") {
    group = "application"
    description = "Runs the server with the debug Spring profile."
    mainClass.set("com.terry.duey.DueyServerApplication")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("spring.profiles.active", "debug")
}

tasks.register<org.springframework.boot.gradle.tasks.run.BootRun>("bootRunStage") {
    group = "application"
    description = "Runs the server with the stage Spring profile."
    mainClass.set("com.terry.duey.DueyServerApplication")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("spring.profiles.active", "stage")
}

tasks.register<org.springframework.boot.gradle.tasks.run.BootRun>("bootRunProd") {
    group = "application"
    description = "Runs the server with the prod Spring profile."
    mainClass.set("com.terry.duey.DueyServerApplication")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("spring.profiles.active", "prod")
}
