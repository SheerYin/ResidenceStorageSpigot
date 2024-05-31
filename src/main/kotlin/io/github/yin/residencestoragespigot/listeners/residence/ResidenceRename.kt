package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceRenameEvent
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.MessageYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object ResidenceRename : Listener {

    // 领地被更改名称 前 时才会触发
    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceRename(event: ResidenceRenameEvent) {
        val oldName = event.oldResidenceName
        val newName = event.newResidenceName

        if (ResidenceMySQLStorage.getResidenceNames().contains(newName)) {
            val player = Bukkit.getPlayer(event.residence.owner) ?: return
            player.sendMessage(MessageYAMLStorage.configuration.getString("command.create-name-already-exists"))
            event.isCancelled = true
            return
        } else {
            ResidenceStorageSpigotMain.scope.launch {
                ResidenceMySQLStorage.updateResidenceName(oldName, newName)
            }
        }
    }
}
