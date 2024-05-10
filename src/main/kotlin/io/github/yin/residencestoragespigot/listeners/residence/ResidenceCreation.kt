package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.event.ResidenceCreationEvent
import io.github.yin.proxyinfospigot.ProxyInfoSpigotMain
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ConfigurationYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import io.github.yin.residencestoragespigot.supports.ResidenceInfo
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.*

object ResidenceCreation : Listener {

    // 领地创建 前 触发，就算创建失败也会触发
    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceCreation(event: ResidenceCreationEvent) {

        val player = event.player
        val names = ResidenceMySQLStorage.getOwnerUUIDResidenceNames(player.uniqueId)
        val residenceName = event.residenceName
        // 玩家领地总数有没有大于 config 的 residences.amount 权限
        // 有没有重名
        if (names.size > numberPermissions(player) || names.contains(residenceName)) {
            event.isCancelled = true
            return
        }

        val ownerUUID = player.uniqueId.toString()
        val owner = player.displayName
        val residence = event.residence
        val permissions = residence.permissions

        Bukkit.getScheduler().runTaskLaterAsynchronously(ResidenceStorageSpigotMain.instance, Runnable {

            // 延迟后才知道领地到底创建成功没有
            if (Residence.getInstance().residenceManager.residences.containsKey(residenceName.lowercase(Locale.getDefault()))) {
                val residenceInfo =
                    ResidenceInfo(
                        residenceName,
                        ownerUUID,
                        owner,
                        permissions.flags,
                        permissions.playerFlags,
                        ProxyInfoSpigotMain.serverName
                    )
                ResidenceMySQLStorage.addResidence(residenceInfo)
            }
        }, 5L)

    }


    private fun numberPermissions(player: Player): Int {
        val section: ConfigurationSection =
            ConfigurationYAMLStorage.fileConfiguration.getConfigurationSection("residences.amount")!!
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

