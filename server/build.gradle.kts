import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    checkstyle
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

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

checkstyle {
    toolVersion = "10.21.4"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
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

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

tasks.register("lint") {
    group = "verification"
    description = "Runs server Java lint checks."
    dependsOn("compileJava", "checkstyleMain", "checkstyleTest")
}

fun BootRun.useDueyGradleEnvironment() {
    listOf(
        "ACCESS_TOKEN_MINUTES",
        "AI_PROVIDER",
        "DB_PASSWORD",
        "DB_URL",
        "DB_USERNAME",
        "DEBUG_USER_ID",
        "GEMINI_API_KEY",
        "GEMINI_MODEL",
        "GOOGLE_CLIENT_ID",
        "JWT_SECRET",
        "REFRESH_TOKEN_DAYS",
        "SERVER_PORT",
    ).forEach { name ->
        providers.gradleProperty(name).orNull?.let { value -> environment(name, value) }
    }
}

tasks.register<BootRun>("bootRunDebug") {
    group = "application"
    description = "Runs the server with the debug Spring profile."
    mainClass.set("com.terry.duey.DueyServerApplication")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("spring.profiles.active", "debug")
    useDueyGradleEnvironment()
}

tasks.register<BootRun>("bootRunStage") {
    group = "application"
    description = "Runs the server with the stage Spring profile."
    mainClass.set("com.terry.duey.DueyServerApplication")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("spring.profiles.active", "stage")
    useDueyGradleEnvironment()
}

tasks.register<BootRun>("bootRunProd") {
    group = "application"
    description = "Runs the server with the prod Spring profile."
    mainClass.set("com.terry.duey.DueyServerApplication")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("spring.profiles.active", "prod")
    useDueyGradleEnvironment()
}
