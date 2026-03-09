plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    implementation("io.javalin:javalin:6.4.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

/**
 * Local helper task if you still want to run the server through Gradle.
 */
tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Run the FreshlyGround HTTP compiler server."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("freshlyground.server.CompilerServer")
}

/**
 * Plain jar metadata.
 */
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "freshlyground.server.CompilerServer"
        )
    }
}

/**
 * Fat jar for Docker / Render deployment.
 */
tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes(
            "Main-Class" to "freshlyground.server.CompilerServer"
        )
    }
}

/**
 * The deployed application should be the HTTP server, not the CLI.
 */
application {
    mainClass.set("freshlyground.server.CompilerServer")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}