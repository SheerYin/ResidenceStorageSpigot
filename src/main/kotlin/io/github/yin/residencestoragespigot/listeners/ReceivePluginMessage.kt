package io.github.yin.residencestoragespigot.listeners

import com.bekvon.bukkit.residence.Residence
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
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
        if (channel != ResidenceStorageSpigotMain.pluginChannel) {
            return
        }

        DataInputStream(ByteArrayInputStream(message)).use { input ->
            val action = input.readUTF()

            when (action) {
                // 标识符 bytes.size bytes
                "residencestorage:identifier" -> {
                    val len = input.readShort()
                    val bytes = ByteArray(len.toInt())
                    input.readFully(bytes)
                    DataInputStream(ByteArrayInputStream(bytes)).use { stream ->
                        val residenceName = stream.readUTF()
                        val claimedResidence =
                            Residence.getInstance().residenceManager.residences[residenceName.lowercase(Locale.getDefault())]
                                ?: return
                        player.teleport(claimedResidence.getTeleportLocation(player, true))
                    }
                }
                // PlayerList ALL a, b, c
                "PlayerList" -> {
                    if (input.readUTF() != "ALL") {
                        return
                    }
                    ResidenceStorageSpigotMain.playerNames.addAll(input.readUTF().split(", "))
                }
                /*
                // GetServers a, b, c
                "GetServers" -> {
                    ResidenceStorageSpigotMain.serverNames.addAll(input.readUTF().split(", "))
                }
                 */
                // GetServers serverName
                "GetServer" -> {
                    ResidenceStorageSpigotMain.serverName = input.readUTF()
                }

                else -> {
                    return
                }
            }

        }

    }


}