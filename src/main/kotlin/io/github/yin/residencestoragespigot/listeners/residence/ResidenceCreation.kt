package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.event.ResidenceCreationEvent
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import io.github.yin.residencestoragespigot.supports.ResidenceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.*

object ResidenceCreation : Listener {

    // 领地创建 前 触发，就算创建失败也会触发
    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceCreation(event: ResidenceCreationEvent) {

        val residence = event.residence
        val residenceName = residence.residenceName
        val ownerUUID = residence.ownerUUID
        val owner = residence.owner
        val permissions = residence.permissions

        ResidenceStorageSpigotMain.scope.launch {
            delay(500)
            // 延迟后才知道领地到底创建成功没有
            if (Residence.getInstance().residenceManager.residences.containsKey(residenceName.lowercase(Locale.getDefault()))) {
                val residenceInfo =
                    ResidenceInfo(
                        residenceName,
                        ownerUUID,
                        owner,
                        permissions.flags,
                        permissions.playerFlags,
                        ResidenceStorageSpigotMain.serverName
                    )
                ResidenceMySQLStorage.addResidence(residenceInfo)
            }
        }

    }




}

