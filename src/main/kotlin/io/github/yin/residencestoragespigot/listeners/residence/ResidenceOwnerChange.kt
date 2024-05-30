package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceOwnerChangeEvent
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object ResidenceOwnerChange : Listener {

    // 领地被更改所有者 前 时才会触发
    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceOwnerChange(event: ResidenceOwnerChangeEvent) {
        val residenceName = event.residence.residenceName
        val ownerUUID = event.newOwnerUuid
        val owner = event.newOwner

        ResidenceStorageSpigotMain.scope.launch {
            ResidenceMySQLStorage.updateResidenceOwner(residenceName, ownerUUID, owner)
        }
    }

}
