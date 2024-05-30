package io.github.yin.residencestoragespigot.storages

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object MessageYAMLStorage {

    private lateinit var path: Path
    fun initialize(file: File) {
        var folder = Paths.get(ConfigurationYAMLStorage.configuration.getString("message.file.path"))
        if (folder.toString().isEmpty()) {
            folder = Paths.get(file.path, "Message")
        } else {
            if (folder.startsWith(Paths.get("plugins"))) {
                folder = file.toPath().parent.resolve(folder.subpath(1, folder.nameCount))
            }
        }
        Files.createDirectories(folder)
        path = folder.resolve("${ConfigurationYAMLStorage.language}.yml")
        if (!Files.exists(path)) {
            val stream = ResidenceStorageSpigotMain.instance.getResource("Message/chinese.yml")
            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    lateinit var configuration: YamlConfiguration
    fun load() {
        configuration = YamlConfiguration.loadConfiguration(path.toFile())
    }
}