package io.github.yin.residencestoragespigot.supports

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import java.util.*

class ResidenceStorageExpansion(plugin: Plugin) : PlaceholderExpansion() {
    companion object {
        val descriptionName = ResidenceStorageSpigotMain.instance.description.name.lowercase(Locale.getDefault())
        val descriptionAuthors = ResidenceStorageSpigotMain.instance.description.authors.joinToString(",")
        val descriptionVersion = ResidenceStorageSpigotMain.instance.description.version
    }

    override fun getIdentifier(): String {
        return descriptionName
    }

    override fun getAuthor(): String {
        return descriptionAuthors
    }

    override fun getVersion(): String {
        return descriptionVersion
    }

    override fun onRequest(offlinePlayer: OfflinePlayer?, parameters: String): String? {
        if (parameters.equals("number", ignoreCase = true)) {

        }

        return null
    }
}