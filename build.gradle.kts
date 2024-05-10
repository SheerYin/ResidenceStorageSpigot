import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm") version "1.9.23"
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
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation(files("libraries/ProxyInfoSpigot.jar"))
    implementation(files("libraries/Residence.jar"))
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

kotlin {
    jvmToolchain(17)
}