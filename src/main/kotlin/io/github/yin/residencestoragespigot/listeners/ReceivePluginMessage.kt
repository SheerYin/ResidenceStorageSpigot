package io.github.yin.residencestoragespigot.listeners

import com.bekvon.bukkit.residence.Residence
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain.Companion.pluginChannel
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.*

object ReceivePluginMessage : PluginMessageListener {
    @EventHandler(priority = EventPriority.NORMAL)
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel == pluginChannel) {
            DataInputStream(ByteArrayInputStream(message)).use { input ->
                val action = input.readUTF().lowercase()
                if (action != ("teleport")) {
                    return
                }

                val residenceName = input.readUTF()
                val claimedResidence =
                    Residence.getInstance().residenceManager.residences[residenceName.lowercase(Locale.getDefault())]
                        ?: return
                player.teleport(claimedResidence.getTeleportLocation(player, true))
            }
        }
    }

}