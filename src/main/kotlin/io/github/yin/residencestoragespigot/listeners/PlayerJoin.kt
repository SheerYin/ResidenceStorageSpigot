package io.github.yin.residencestoragespigot.listeners

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object PlayerJoin : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {

        ResidenceStorageSpigotMain.scope.launch {
            val player = event.player
            delay(500)
            ResidenceStorageSpigotMain.instance.sendByteServerName(player)

            /*
            if (ResidenceStorageSpigotMain.serverNames.isEmpty()) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                DataOutputStream(byteArrayOutputStream).use { output ->
                    output.writeUTF("GetServers")
                }
                player.sendPluginMessage(ResidenceStorageSpigotMain.instance, ResidenceStorageSpigotMain.pluginChannel, byteArrayOutputStream.toByteArray())
            }
             */

            ResidenceStorageSpigotMain.instance.sendBytePlayerNames(player)

        }

    }


}