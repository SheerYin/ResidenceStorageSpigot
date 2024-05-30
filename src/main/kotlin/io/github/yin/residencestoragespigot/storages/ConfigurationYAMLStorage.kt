package io.github.yin.residencestoragespigot.storages

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object ConfigurationYAMLStorage {

    private lateinit var path: Path
    fun initialize(file: File) {
        path = file.toPath().resolve("config.yml")
        if (!Files.exists(path)) {
            ResidenceStorageSpigotMain.instance.saveResource("config.yml", true)
        }
    }

    lateinit var configuration: YamlConfiguration
    lateinit var language: String
    fun load() {
        configuration = YamlConfiguration.loadConfiguration(path.toFile())
        language = configuration.getString("message.language") ?: "chinese"
    }
}