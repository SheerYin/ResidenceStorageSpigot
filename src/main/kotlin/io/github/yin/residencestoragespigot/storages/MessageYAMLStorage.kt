package io.github.yin.residencestoragespigot.storages

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object MessageYAMLStorage {

    private lateinit var folder: Path
    private lateinit var path: Path
    lateinit var language: String

    fun initialization(file: File) {
        val custom = Paths.get(ConfigurationYAMLStorage.fileConfiguration.getString("messages.file.path"))
        // 如果是空则使用默认路径
        if (custom.toString().isEmpty()) {
            folder = Paths.get(file.path, "Messages")
        } else {
            if (custom.startsWith(Paths.get("plugins"))) {
                folder = file.toPath().parent.resolve(custom.subpath(1, custom.nameCount))
            }
        }
        Files.createDirectories(folder)
        language = ConfigurationYAMLStorage.fileConfiguration.getString("messages.language") ?: "chinese"
        path = folder.resolve("$language.yml")
        // 示例文件是必须存在的
        if (!Files.exists(path)) {
            val stream = ResidenceStorageSpigotMain.instance.getResource("Messages/chinese.yml")
            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING)
        }

    }

    lateinit var fileConfiguration: FileConfiguration
    fun load() {
        fileConfiguration = YamlConfiguration.loadConfiguration(path.toFile())
    }
}