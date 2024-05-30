package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceFlagChangeEvent
import com.bekvon.bukkit.residence.event.ResidenceFlagEvent.FlagType
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagState
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object ResidenceFlagChange : Listener {

    // Flags 被修改 前 时才会触发
    // 同时 ResidenceCreation 也会触发，因为创建领地时同时在初始化 Flags
    // 但领地没创建完之前，获取的 residenceName 都是 null
    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceFlagChange(event: ResidenceFlagChangeEvent) {
        val residenceName = event.residence.residenceName ?: return

        ResidenceStorageSpigotMain.scope.launch {
            when (event.flagType) {
                FlagType.RESIDENCE -> {
                    val state = when (event.newState) {
                        FlagState.TRUE -> true
                        FlagState.FALSE -> false
                        else -> null
                    }
                    state?.let {
                        ResidenceMySQLStorage.setResidenceFlags(residenceName, event.flag, it)
                    } ?: ResidenceMySQLStorage.removeResidenceFlags(residenceName, event.flag)
                }

                FlagType.PLAYER -> {
                    val state = when (event.newState) {
                        FlagState.TRUE -> true
                        FlagState.FALSE -> false
                        else -> null
                    }
                    val playerUUID = event.player.uniqueId
                    state?.let {
                        ResidenceMySQLStorage.setPlayerFlags(residenceName, playerUUID, event.flag, it)
                    } ?: ResidenceMySQLStorage.removePlayerFlags(residenceName, playerUUID, event.flag)
                }

                else -> {}
            }
        }
    }


}
