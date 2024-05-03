package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.event.ResidenceCommandEvent
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.*


object ResidenceCommand : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceCommand(event: ResidenceCommandEvent) {
        val player = event.sender as? Player ?: return
        val command = event.command.lowercase(Locale.getDefault())
        if (command != "residence" && command != "res") {
            return
        }

        val args = event.args
        if (args.isEmpty() || args.size < 2) {
            return
        }

        val subCommand = args[0].lowercase()
        val residenceName = args[1]

        when (subCommand) {
            "teleport", "tp" -> {

                val uuid = player.uniqueId
                if (Residence.getInstance().playerManager.getResidenceList(uuid).contains(residenceName)) {
                    return
                }

                val residenceInfo = ResidenceMySQLStorage.getResidence(residenceName) ?: return
                val ownerUUID = uuid.toString()
                if (residenceInfo.ownerUUID == ownerUUID || residenceInfo.residenceFlags["tp"] == true || residenceInfo.playerFlags[ownerUUID]?.get(
                        "tp"
                    ) == true
                ) {
                    event.isCancelled = true
                    Bukkit.getScheduler().runTaskAsynchronously(ResidenceStorageSpigotMain.instance, Runnable {

                        val byteArrayOutputStream = ByteArrayOutputStream()
                        val output = DataOutputStream(byteArrayOutputStream)
                        output.writeUTF(residenceName)
                        output.writeUTF(residenceInfo.serverName)
                        player.sendPluginMessage(
                            ResidenceStorageSpigotMain.instance,
                            ResidenceStorageSpigotMain.pluginChannel,
                            byteArrayOutputStream.toByteArray()
                        )

                    })
                }
            }

            "list" -> {
                // 处理列表命令
            }
        }
    }
}
