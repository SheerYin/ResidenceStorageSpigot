package io.github.yin.residencestoragespigot.supports

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin

class BatchCommandExpansion(plugin: Plugin) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return ResidenceStorageSpigotMain.lowercaseName
    }

    override fun getAuthor(): String {
        return ResidenceStorageSpigotMain.pluginAuthors.joinToString(",")
    }

    override fun getVersion(): String {
        return ResidenceStorageSpigotMain.pluginVersion
    }

    override fun onRequest(offlinePlayer: OfflinePlayer?, parameters: String): String? {
        val player = (offlinePlayer?.player) ?: return null
        when {
            parameters.equals("amount", ignoreCase = true) -> {
                return ResidenceMySQLStorage.getOwnerResidenceNames(player.uniqueId).size.toString()
            }
            parameters.equals("maximum", ignoreCase = true) -> {
                return Limit.numberPermissions(player).toString()
            }
            else -> {
                return null
            }
        }
    }
}