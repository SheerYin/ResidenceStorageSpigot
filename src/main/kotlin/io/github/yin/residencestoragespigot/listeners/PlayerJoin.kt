package io.github.yin.residencestoragespigot.listeners

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.supports.SendByte
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
            SendByte.serverName(player)
            SendByte.playerNames(player)
        }

    }


}