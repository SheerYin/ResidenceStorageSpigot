package io.github.yin.residencestoragespigot.listeners

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object PlayerJoin : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {

        Bukkit.getScheduler().runTaskLater(ResidenceStorageSpigotMain.instance, Runnable {
            val player = event.player

            if (ResidenceStorageSpigotMain.serverName.isEmpty()) {
                ResidenceStorageSpigotMain.instance.sendByteServerName(player)
            }

            /*
            if (ResidenceStorageSpigotMain.serverNames.isEmpty()) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                DataOutputStream(byteArrayOutputStream).use { output ->
                    output.writeUTF("GetServers")
                }
                player.sendPluginMessage(ResidenceStorageSpigotMain.instance, ResidenceStorageSpigotMain.pluginChannel, byteArrayOutputStream.toByteArray())
            }
             */

            if (ResidenceStorageSpigotMain.playerNames.isEmpty()) {
                ResidenceStorageSpigotMain.instance.sendBytePlayerNames(player)
            }
        }, 20)

    }


}