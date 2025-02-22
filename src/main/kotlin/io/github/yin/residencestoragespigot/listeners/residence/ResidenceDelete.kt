package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceDeleteEvent
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object ResidenceDelete : Listener {

    // 领地被删除 前 时才会触发
    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceDelete(event: ResidenceDeleteEvent) {
        val residenceName = event.residence.residenceName

        ResidenceStorageSpigotMain.scope.launch {
            ResidenceMySQLStorage.removeResidence(residenceName)
        }
    }
}
