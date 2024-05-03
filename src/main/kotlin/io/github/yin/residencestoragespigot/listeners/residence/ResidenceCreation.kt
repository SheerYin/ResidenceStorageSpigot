package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.event.ResidenceCreationEvent
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import io.github.yin.residencestoragespigot.supports.ResidenceInfo
import io.github.yin.servernamespigot.ServerNameSpigotMain
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.*


object ResidenceCreation : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceCreation(event: ResidenceCreationEvent) {

        val residenceName = event.residenceName
        if (ResidenceMySQLStorage.hasResidenceName(residenceName)) {
            event.isCancelled = true
            return
        }

        val residence = event.residence
        val ownerUUID = residence.ownerUUID.toString()
        val owner = residence.owner
        val permissions = residence.permissions

        Bukkit.getScheduler().runTaskLaterAsynchronously(ResidenceStorageSpigotMain.instance, Runnable {
            if (Residence.getInstance().residenceManager.residences.containsKey(residenceName.lowercase(Locale.getDefault()))) {

                val residenceInfo =
                    ResidenceInfo(
                        residenceName,
                        ownerUUID,
                        owner,
                        permissions.flags,
                        permissions.playerFlags,
                        ServerNameSpigotMain.serverName
                    )
                ResidenceMySQLStorage.addResidence(residenceInfo)
            }
        }, 5L)

    }

}

