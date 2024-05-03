package io.github.yin.residencestoragespigot.storages

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


object ConfigurationYAMLStorage {

    private lateinit var path: Path
    fun initialization(file: File) {
        path = file.toPath().resolve("config.yml")
        if (!Files.exists(path)) {
            ResidenceStorageSpigotMain.instance.saveResource("config.yml", true)
        }
    }

    lateinit var fileConfiguration: FileConfiguration
    fun load() {
        fileConfiguration = YamlConfiguration.loadConfiguration(path.toFile())
    }


}