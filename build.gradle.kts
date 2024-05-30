import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

var pluginVersion = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"
group = "io.github.yin.residencestoragespigot"
version = ""

repositories {
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // implementation(files("libraries/ProxyInfoSpigot.jar"))
    compileOnly(files("libraries/Residence.jar"))
}

tasks.shadowJar {
    archiveVersion.set("")
    archiveClassifier.set("shadow-j8")
//    relocate("kotlin", "relocated.kotlin")
//    relocate("org.jetbrains", "relocated.jetbrains")
//    relocate("org.intellij", "relocated.intellij")
}

tasks.register("writePluginVersion") {
    doLast {
        file("src/main/resources/plugin.yml").apply {
            writeText(readText().replace("{pluginVersion}", pluginVersion))
        }
    }
}

tasks.register("restorePluginVersion") {
    doLast {
        file("src/main/resources/plugin.yml").apply {
            writeText(readText().replace(pluginVersion, "{pluginVersion}"))
        }
    }
}

tasks.named("compileJava") {
    dependsOn("writePluginVersion")
}

tasks.named("build") {
    finalizedBy("restorePluginVersion")
}

tasks.named("shadowJar") {
    finalizedBy("restorePluginVersion")
}

kotlin {
    // jvmToolchain(8)
    jvmToolchain(17)
}

tasks.withType<JavaCompile> {
//    sourceCompatibility = "1.8"
//    targetCompatibility = "1.8"

    sourceCompatibility = "17"
    targetCompatibility = "17"
}