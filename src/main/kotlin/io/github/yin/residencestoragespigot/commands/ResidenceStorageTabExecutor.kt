package io.github.yin.residencestoragespigot.commands

import com.bekvon.bukkit.residence.Residence
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.ConfigurationYAMLStorage
import io.github.yin.residencestoragespigot.storages.MessageYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import io.github.yin.residencestoragespigot.supports.*
import kotlinx.coroutines.launch
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList

object ResidenceStorageTabExecutor : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): Boolean {
        when (arguments.size) {
            0 -> {
                if(permissionMessage(sender, "${ResidenceStorageSpigotMain.lowercaseName}.command.help")) {
                    processHelp(sender)
                }
            }
            1 -> {
                when {
                    suggestion(sender, arguments[0], "help") -> {
                        processHelp(sender)
                    }
                    suggestion(sender,arguments[0], "list") -> {
                        (sender as? Player)?.let { player ->
                            ResidenceStorageSpigotMain.scope.launch {
                                processList(player, player.displayName, "1")
                            }
                        } ?: sender.sendMessage(MessageYAMLStorage.configuration.getString("command.only-player-execute"))
                    }
                    suggestion(sender, arguments[0], "listall") -> {
                        ResidenceStorageSpigotMain.scope.launch {
                            processListAll(sender, "1")
                        }
                    }
                    suggestion(sender, arguments[0], "import") -> {
                        (sender as? Player)?.let { player ->
                            ResidenceStorageSpigotMain.scope.launch {
                                processImport(player)
                            }
                        } ?: sender.sendMessage(MessageYAMLStorage.configuration.getString("command.only-player-execute"))
                    }
                    suggestion(sender, arguments[0], "reload") -> {
                        ResidenceStorageSpigotMain.scope.launch {
                            ConfigurationYAMLStorage.initialize(ResidenceStorageSpigotMain.instance.dataFolder)
                            ConfigurationYAMLStorage.load()
                            MessageYAMLStorage.initialize(ResidenceStorageSpigotMain.instance.dataFolder)
                            MessageYAMLStorage.load()
                            sender.sendMessage(MessageYAMLStorage.configuration.getString("command.reload"))
                        }
                    }
                }
            }
            2 -> {
                when {
                    suggestion(sender, arguments[0], "list") -> {
                        Bukkit.getScheduler().runTaskAsynchronously(ResidenceStorageSpigotMain.instance, Runnable {
                            processList(sender, arguments[1], "1")
                        })
                    }
                    suggestion(sender, arguments[0], "listall") -> {
                        Bukkit.getScheduler().runTaskAsynchronously(ResidenceStorageSpigotMain.instance, Runnable {
                            processListAll(sender, arguments[1])
                        })
                    }
                }
            }

            3 -> {
                when {
                    suggestion(sender, arguments[0], "list") -> {
                        Bukkit.getScheduler().runTaskAsynchronously(ResidenceStorageSpigotMain.instance, Runnable {
                            processList(sender, arguments[1], arguments[2])
                        })
                    }
                    suggestion(sender, arguments[0], "teleport") -> {
                        Bukkit.getScheduler().runTaskAsynchronously(ResidenceStorageSpigotMain.instance, Runnable {
                            processTeleport(sender, arguments[1], arguments[2])
                        })
                    }
                }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): List<String>? {
        when (arguments.size) {
            1 -> {
                return listMatches(arguments[0], listOf("help", "list", "listall", "teleport", "import", "reload"))
            }
            2 -> {
                when {
                    arguments[0].equals("list", ignoreCase = true) -> {
                        return null
                    }
                    arguments[0].equals("listall", ignoreCase = true) -> {
                        return listOf("<page>")
                    }
                    arguments[0].equals("teleport", ignoreCase = true) -> {
                        if (Cooldown.globalUse(Duration.ofSeconds(5))) {
                            val player = Bukkit.getOnlinePlayers().firstOrNull() ?: return null
                            SendByte.playerNames(player)
                        }
                        return listMatches(arguments[1], ResidenceStorageSpigotMain.playerNames)
                    }
                }
            }
            3 -> {
                when {
                    arguments[0].equals("list", ignoreCase = true) -> {
                        return listOf("<page>")
                    }
                    arguments[0].equals("teleport", ignoreCase = true) -> {
                        return listMatches(arguments[2], ResidenceMySQLStorage.getResidenceNames())
                    }
                }
            }
        }
        return emptyList()
    }

    private fun listMatches(argument: String, suggest: Iterable<String>): List<String> {
        return suggest.filter { it.contains(argument) }
    }

    private fun permissionMessage(sender: CommandSender, permission: String): Boolean {
        if (sender.hasPermission(permission)) {
            return true
        }
        sender.sendMessage(
            IndexReplace.replace(MessageYAMLStorage.configuration.getString("permission.no-permission")!!, permission)
        )
        return false
    }

    private fun suggestion(sender: CommandSender, argument: String, vararg suggest: String): Boolean {
        val lowerCaseArgument = argument.lowercase(Locale.getDefault())
        if (lowerCaseArgument in suggest) {
            return permissionMessage(sender, "${ResidenceStorageSpigotMain.lowercaseName}.command.$lowerCaseArgument")
        }
        return false
    }


    private fun processHelp(sender: CommandSender) {
        for (text in MessageYAMLStorage.configuration.getStringList("command.help")) {
            sender.sendMessage(text)
        }
    }


    private fun processImport(player: CommandSender) {
        val residenceManager = Residence.getInstance().residenceManager

        val mysqlNames = ResidenceMySQLStorage.getResidenceNames()
        val localNames = ArrayList<String>()
        val residenceInfos = ArrayList<ResidenceInfo>()

        for (residence in residenceManager.residences) {
            val value = residence.value
            localNames.add(value.residenceName)
            residenceInfos.add(ResidenceInfo(value.residenceName, value.ownerUUID, value.owner, value.permissions.flags, value.permissions.playerFlags, ResidenceStorageSpigotMain.serverName))
        }

        val duplicates = mutableListOf<String>()
        val set1 = mysqlNames.toSet()
        val set2 = localNames.toSet()

        for (element in set1) {
            if (element in set2) {
                duplicates.add(element)
            }
        }

        if (duplicates.isNotEmpty()) {
            player.sendMessage(
                IndexReplace.replace(
                    MessageYAMLStorage.configuration.getString("command.import-conflict")!!,
                    duplicates.toString()
                )
            )
        } else {
            ResidenceMySQLStorage.addResidences(residenceInfos)
            player.sendMessage(MessageYAMLStorage.configuration.getString("command.import-complete"))
        }
    }


    private fun processList(sender: CommandSender, playerName: String, pageString: String) {
        val page: Int = pageString.toIntOrNull() ?: run {
            sender.sendMessage(MessageYAMLStorage.configuration.getString("command.page-invalid"))
            return
        }

        val names = ResidenceMySQLStorage.getOwnerResidenceNames(playerName)
        if (names.isEmpty()) {
            sender.sendMessage(
                IndexReplace.replace(
                    MessageYAMLStorage.configuration.getString("command.player-page-no-residence")!!,
                    playerName
                )
            )
            return
        }

        // 从缓存获取数据
        var list = ResidencePage.playerPage[playerName]
        // 如果获取不到则从 mysql 加载并排序和分页
        // 如果获取到了，判断是不是第一页，如果是，则再重新从 mysql 加载数据到缓存并排序和分页
        if (list == null) {
            val sortedNames = names.sortedBy { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            ResidencePage.playerPage[playerName] = ResidencePage.split(sortedNames, 10)
            list = ResidencePage.playerPage[playerName]!!
        } else {
            if (page == 1) {
                val sortedNames = names.sortedBy { it -> it.filter { it.isDigit() }.toIntOrNull() ?: 0 }
                ResidencePage.playerPage[playerName] = ResidencePage.split(sortedNames, 10)
                list = ResidencePage.playerPage[playerName]!!
            }
        }

        // 判断页数是不是无效的
        if (page !in 1..list.size) {
            sender.sendMessage(
                IndexReplace.replace(
                    MessageYAMLStorage.configuration.getString("command.player-page-no")!!, page.toString()
                )
            )
            return
        }

        sender.sendMessage(MessageYAMLStorage.configuration.getString("command.player-page-header"))

        // 发送当前页的列表
        for (residenceName in list[page - 1]) {
            val text = IndexReplace.replace(
                MessageYAMLStorage.configuration.getString("command.player-page-list")!!,
                playerName,
                residenceName
            )
            val baseComponents = ComponentSerializer.parse(text)
            sender.spigot().sendMessage(*baseComponents)
        }

        // 为了避免手贱访问第 1 页而给 mysql 增加负担
        // 不使用该限定第一页和后一页
        // val prevPage = maxOf(1, page - 1)
        // val nextPage = minOf(list.size, page + 1)

        // 发送页脚
        val text = IndexReplace.replace(
            MessageYAMLStorage.configuration.getString("command.player-page-footer")!!,
            playerName,
            page.toString(),
            (page - 1).toString(),
            (page + 1).toString(),
            list.size.toString(),
        )
        val baseComponents = ComponentSerializer.parse(text)
        sender.spigot().sendMessage(*baseComponents)
    }

    private fun processListAll(sender: CommandSender, pageString: String) {
        val page: Int = pageString.toIntOrNull() ?: run {
            sender.sendMessage(MessageYAMLStorage.configuration.getString("command.page-invalid"))
            return
        }

        val residenceInfos = ResidenceMySQLStorage.getResidences()
        if (residenceInfos.isEmpty()) {
            sender.sendMessage(IndexReplace.replace(MessageYAMLStorage.configuration.getString("command.all-page-no-residence")!!))
            return
        }

        if (ResidencePage.allPage.getOrNull(1) == null) {
            val map: Map<String, ResidenceInfo> = residenceInfos.sortedBy { info -> info.residenceName.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }.associateBy { it.residenceName }.toMutableMap()
            ResidencePage.allPage = ResidencePage.allSplit(map, 10)
        } else {
            if (page == 1) {
                val map: Map<String, ResidenceInfo> = residenceInfos.sortedBy { info -> info.residenceName.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }.associateBy { it.residenceName }.toMutableMap()
                ResidencePage.allPage = ResidencePage.allSplit(map, 10)
            }
        }

        // 判断页数是不是无效的
        if (page !in 1..ResidencePage.allPage.size) {
            sender.sendMessage(
                IndexReplace.replace(
                    MessageYAMLStorage.configuration.getString("command.player-page-no")!!, page.toString()
                )
            )
            return
        }

        sender.sendMessage(MessageYAMLStorage.configuration.getString("command.all-page-header"))

        // 发送当前页的列表
        for (residence in ResidencePage.allPage[page - 1]) {
            val value = residence.value
            val text = IndexReplace.replace(
                MessageYAMLStorage.configuration.getString("command.all-page-list")!!,
                sender.name,
                residence.key,
                value.owner,
                value.serverName
            )
            val baseComponents = ComponentSerializer.parse(text)
            sender.spigot().sendMessage(*baseComponents)
        }

        // 发送页脚
        val text = IndexReplace.replace(
            MessageYAMLStorage.configuration.getString("command.all-page-footer")!!,
            page.toString(),
            (page - 1).toString(),
            (page + 1).toString(),
            (ResidencePage.allPage.size).toString()
        )
        val baseComponents = ComponentSerializer.parse(text)
        sender.spigot().sendMessage(*baseComponents)

    }


    // 该方法是无视权限直接传送
    // 涉及复杂的权限判断的去看 ResidenceCommand 的 teleport()
    private fun processTeleport(sender: CommandSender, playerName: String, residenceName: String) {

        val player = Bukkit.getOnlinePlayers().firstOrNull() ?: run {
            sender.sendMessage(MessageYAMLStorage.configuration.getString("command.teleport-no-online-player"))
            return
        }

        if (playerName !in ResidenceStorageSpigotMain.playerNames) {
            sender.sendMessage(
                IndexReplace.replace(MessageYAMLStorage.configuration.getString("command.not-player")!!, playerName)
            )
            return
        }

        val residenceInfo = ResidenceMySQLStorage.getResidence(residenceName) ?: run {
            sender.sendMessage(
                IndexReplace.replace(
                    MessageYAMLStorage.configuration.getString("command.teleport-no-residence")!!,
                    residenceName
                )
            )
            return
        }

        val serverName = residenceInfo.serverName

        val byteArrayOutputStream = ByteArrayOutputStream()
        // 跨服传送
        DataOutputStream(byteArrayOutputStream).use { output ->
            output.writeUTF("ConnectOther")
            output.writeUTF(playerName)
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
                    writeUTF(playerName)
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

        sender.sendMessage(MessageYAMLStorage.configuration.getString("command.teleport"))
    }

}

