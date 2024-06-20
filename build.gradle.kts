import xyz.jpenilla.resourcefactory.bukkit.Permission
import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm") version "2.0.0"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.1.1"
}

val lowercaseName = project.name.lowercase(Locale.getDefault())
group = "io.github.yin.$lowercaseName"
version = ""; val pluginVersion = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    maven("https://libraries.minecraft.net/")
    maven("https://repo.codemc.io/repository/nms/")

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.20.6-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.6")

    // implementation(files("libraries/ProxyInfoSpigot.jar"))
    compileOnly(files("libraries/Residence.jar"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

bukkitPluginYaml {
    apiVersion = "1.16"
    version = pluginVersion
    main = "$group.${project.name}Main"
    authors.add("尹")
    depend.add("Residence")
    softDepend.add("PlaceholderAPI")
    prefix = "领地储存"
    libraries = listOf("org.jetbrains.kotlin:kotlin-stdlib:2.0.0", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1", "com.zaxxer:HikariCP:5.1.0")

    val pre = "${lowercaseName}.command"
    commands {
        register(lowercaseName) {
            aliases = listOf("rs")
            permission = pre
        }
    }
    permissions {
        register(pre) {
            default = Permission.Default.OP
        }
        register("${pre}.help") {
            default = Permission.Default.OP
        }
        register("${pre}.list") {
            default = Permission.Default.OP
        }
        register("${pre}.listall") {
            default = Permission.Default.OP
        }
        register("${pre}.teleport") {
            default = Permission.Default.OP
        }
        register("${pre}.reload") {
            default = Permission.Default.OP
        }
    }
}

kotlin {
    jvmToolchain(17)
}