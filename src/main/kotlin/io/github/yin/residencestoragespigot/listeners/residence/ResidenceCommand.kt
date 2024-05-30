package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.event.ResidenceCommandEvent
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.MessageYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import io.github.yin.residencestoragespigot.supports.ResidencePage
import io.github.yin.residencestoragespigot.supports.TextProcess
import kotlinx.coroutines.launch
import net.md_5.bungee.chat.ComponentSerializer
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

        val arguments = event.args
        if (arguments.isEmpty()) {
            return
        }


        when (arguments.size) {
            1 -> {
                val argument = arguments[0].lowercase(Locale.getDefault())
                if (argument == "list") {
                    listOne(player, event)
                }
            }

            2 -> {
                if (arguments[0].lowercase() == "tp") {
                    teleport(player, arguments[1], event)
                }
            }
        }
    }


    private fun listOne(player: Player, event: ResidenceCommandEvent) {
        val playerName = player.name

        val names = ResidenceMySQLStorage.getOwnerResidenceNames(playerName)
        if (names.isEmpty()) {
            return
        }

        event.isCancelled = true
        ResidencePage.playerPage[playerName] = ResidencePage.split(names.sortedBy {
            it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0
        }, 10)

        val list = ResidencePage.playerPage[playerName] ?: run {
            player.sendMessage(
                TextProcess.replace(
                    MessageYAMLStorage.configuration.getString("command.player-page-no-residence")!!,
                    playerName
                )
            )
            return
        }

        player.sendMessage(MessageYAMLStorage.configuration.getString("command.player-page-header"))
        for (name in list[0]) {
            val text = TextProcess.replace(
                MessageYAMLStorage.configuration.getString("command.player-page-list")!!,
                name,
                playerName
            )
            val baseComponents = ComponentSerializer.parse(text)
            player.spigot().sendMessage(*baseComponents)
        }

        val text = TextProcess.replace(
            MessageYAMLStorage.configuration.getString("command.player-page-footer")!!,
            playerName,
            "1",
            (list.size).toString(),
            "0",
            "2"
        )
        val baseComponents = ComponentSerializer.parse(text)
        player.spigot().sendMessage(*baseComponents)
    }


    private fun teleport(player: Player, residenceName: String, event: ResidenceCommandEvent) {
        val uuid = player.uniqueId
        if (Residence.getInstance().playerManager.getResidenceList(uuid).contains(residenceName)) {
            return
        }

        val residenceInfo = ResidenceMySQLStorage.getResidence(residenceName) ?: return

        val ownerUUID = uuid.toString()
        if (residenceInfo.ownerUUID == ownerUUID || residenceInfo.residenceFlags["tp"] == true || residenceInfo.playerFlags[ownerUUID]?.get("tp") == true) {
            event.isCancelled = true
            ResidenceStorageSpigotMain.scope.launch {
                val serverName = residenceInfo.serverName

                val byteArrayOutputStream = ByteArrayOutputStream()
                // 跨服传送
                DataOutputStream(byteArrayOutputStream).use { output ->
                    output.writeUTF("Connect")
                    output.writeUTF(serverName)
                }
                player.sendPluginMessage(
                    ResidenceStorageSpigotMain.instance,
                    ResidenceStorageSpigotMain.pluginChannel,
                    byteArrayOutputStream.toByteArray()
                )

                byteArrayOutputStream.reset()
                // 消息
                DataOutputStream(byteArrayOutputStream).use { output ->
                    output.writeUTF("Forward")
                    output.writeUTF(serverName)
                    output.writeUTF("residencestorage:identifier")
                    val byte = ByteArrayOutputStream().apply {
                        DataOutputStream(this).run {
                            writeUTF(residenceName)
                            flush()
                        }
                    }.toByteArray()
                    output.writeShort(byte.size)
                    output.write(byte)
                }
                player.sendPluginMessage(
                    ResidenceStorageSpigotMain.instance,
                    ResidenceStorageSpigotMain.pluginChannel,
                    byteArrayOutputStream.toByteArray()
                )
            }
        }
    }


}
