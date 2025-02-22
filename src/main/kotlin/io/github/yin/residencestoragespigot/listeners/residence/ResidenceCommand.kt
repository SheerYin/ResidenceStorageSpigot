package io.github.yin.residencestoragespigot.listeners.residence

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.event.ResidenceCommandEvent
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.MessageYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import io.github.yin.residencestoragespigot.supports.IndexReplace
import io.github.yin.residencestoragespigot.supports.Limit
import io.github.yin.residencestoragespigot.supports.ResidencePage
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
        val arguments = event.args

        val list = listOf("residence", "res")
        if (command in list) {
            when (arguments.size) {
                1 -> {
                    val argument = arguments[0].lowercase(Locale.getDefault())
                    if (argument == "list") {
                        listOne(player, event)
                    }
                }
                2 -> {
                    when {
                        arguments[0].lowercase() == "tp" -> {
                            teleport(player, arguments[1], event)
                        }
                        arguments[0].lowercase() == "create" -> {
                            val names = ResidenceMySQLStorage.getResidenceNames().map { it.lowercase(Locale.getDefault()) }
                            if (arguments[1].lowercase() in names) {
                                player.sendMessage(MessageYAMLStorage.configuration.getString("command.create-name-already-exists"))
                                event.isCancelled = true
                                return
                            }
                            val self = ResidenceMySQLStorage.getOwnerResidenceNames(player.uniqueId)
                            // 玩家领地总数有没有大于 config 的 residences.amount 权限
                            val number = Limit.numberPermissions(player)
                            if (self.size >= number) {
                                player.sendMessage(
                                    IndexReplace.replace(
                                        MessageYAMLStorage.configuration.getString("command.create-amount-limit")!!,
                                        (self.size).toString(),
                                        number.toString()
                                    )
                                )
                                event.isCancelled = true
                                return
                            }
                        }
                    }
                }
            }
        }

        //防管理员
        if (command == "resadmin") {
            if (arguments.size == 2 && arguments[0] == "create") {
                val names = ResidenceMySQLStorage.getResidenceNames().map { it.lowercase(Locale.getDefault()) }
                if (arguments[1].lowercase() in names) {
                    player.sendMessage(MessageYAMLStorage.configuration.getString("command.create-name-already-exists"))
                    event.isCancelled = true
                    return
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


        val list = ResidencePage.playerPage[playerName]
        if (list == null) {
            player.sendMessage(
                IndexReplace.replace(
                    MessageYAMLStorage.configuration.getString("command.player-page-no-residence")!!,
                    playerName
                )
            )
            return
        }

        player.sendMessage(MessageYAMLStorage.configuration.getString("command.player-page-header"))
        for (name in list[0]) {
            val text = IndexReplace.replace(
                MessageYAMLStorage.configuration.getString("command.player-page-list")!!,
                playerName,
                name
            )
            val baseComponents = ComponentSerializer.parse(text)
            player.spigot().sendMessage(*baseComponents)
        }

        val text = IndexReplace.replace(
            MessageYAMLStorage.configuration.getString("command.player-page-footer")!!,
            playerName,
            "1",
            "0",
            "2",
            (list.size).toString()
        )
        val baseComponents = ComponentSerializer.parse(text)
        player.spigot().sendMessage(*baseComponents)
    }


    private fun teleport(player: Player, residenceName: String, event: ResidenceCommandEvent) {

        if (Residence.getInstance().residenceManager.residences.contains(residenceName.lowercase(Locale.getDefault()))) {
            return
        }

        val residenceInfo = ResidenceMySQLStorage.getResidence(residenceName) ?: return

        val ownerUUID = player.uniqueId
        if (residenceInfo.ownerUUID == ownerUUID || residenceInfo.residenceFlags["tp"] == true || residenceInfo.playerFlags[ownerUUID.toString()]?.get("tp") == true) {
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
                            writeUTF(player.name)
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
