package io.github.yin.residencestoragespigot.supports

import io.github.yin.residencestoragespigot.storages.ConfigurationYAMLStorage
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

object Limit {

    fun numberPermissions(player: Player): Int {
        val section: ConfigurationSection = ConfigurationYAMLStorage.configuration.getConfigurationSection("residence.amount")!!
        val map: Map<String, Int> = section.getKeys(false).associateBy({ it }, { section.getInt(it) })

        val sorted = map.entries.sortedByDescending { it.value }
        for (entry in sorted) {
            if (player.hasPermission("residencestoragespigot.amount." + entry.key)) {
                return entry.value
            }
        }
        return 0
    }

}