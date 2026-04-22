plugins {
    kotlin("jvm") version "2.1.20"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

application {
    mainClass.set("TesterKt")
}

// Diagnostic task for file lock detection
tasks.register("checkFileLock") {
    doLast {
        val file = file("build/distributions/maidLang-1.0-SNAPSHOT.zip")
        logger.lifecycle("Checking lock status for ${file.absolutePath}")
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                logger.lifecycle("File was NOT locked (successfully deleted)")
                // Recreate empty file to avoid breaking subsequent tasks
                file.createNewFile()
            } else {
                logger.lifecycle("File is LOCKED (could not delete)")
                logger.lifecycle("This confirms the hypothesis that the file is locked by another process.")
            }
        } else {
            logger.lifecycle("File does not exist")
        }
    }
}

// Add dependency so distZip runs after checking lock (optional)
tasks.named("distZip") {
    dependsOn("checkFileLock")
}