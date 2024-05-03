package io.github.yin.residencestoragespigot.commands

import com.bekvon.bukkit.residence.Residence
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.storages.MessageYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import io.github.yin.residencestoragespigot.supports.ResidenceInfo
import io.github.yin.residencestoragespigot.supports.TextProcess
import io.github.yin.servernamespigot.ServerNameSpigotMain
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

object ResidenceStorageTabExecutor : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        arguments: Array<out String>
    ): Boolean {
        when (arguments.size) {
            0 -> {
                for (text in MessageYAMLStorage.fileConfiguration.getStringList("command.help")) {
                    sender.sendMessage(text)
                }
            }

            1 -> {
                when {
                    suggestion(arguments[0], "help", sender) -> {
                        for (text in MessageYAMLStorage.fileConfiguration.getStringList("command.help")) {
                            sender.sendMessage(text)
                        }
                    }

                    suggestion(arguments[0], "import", sender) -> {
                        (sender as? Player)?.let { player ->
                            val residenceManager = Residence.getInstance().residenceManager

                            val mysqlNames = ResidenceMySQLStorage.getResidenceNames()
                            val localNames = ArrayList<String>()
                            val residenceInfos = ArrayList<ResidenceInfo>()

                            for (residence in residenceManager.residences) {
                                val value = residence.value
                                localNames.add(value.residenceName)
                                residenceInfos.add(
                                    ResidenceInfo(
                                        value.residenceName,
                                        value.ownerUUID.toString(),
                                        value.residenceName,
                                        value.permissions.flags,
                                        value.permissions.playerFlags,
                                        ServerNameSpigotMain.serverName
                                    )
                                )
                            }

                            val conflicts = conflictNames(mysqlNames, localNames)
                            if (conflicts.isNotEmpty()) {
                                player.sendMessage(
                                    TextProcess.replace(
                                        MessageYAMLStorage.fileConfiguration.getString("command.import-conflict")!!,
                                        conflicts.toString()
                                    )
                                )
                            } else {
                                ResidenceMySQLStorage.addResidences(residenceInfos)
                                player.sendMessage(MessageYAMLStorage.fileConfiguration.getString("command.import-complete"))
                            }
                        } ?: run {
                            sender.sendMessage(MessageYAMLStorage.fileConfiguration.getString("command.only-player-execute"))
                        }
                    }

                }

            }

        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        arguments: Array<out String>
    ): List<String> {
        return when (arguments.size) {
            1 -> {
                listMatches(arguments[0], mutableListOf("help", "import"))
            }

            else -> {
                emptyList()
            }
        }
    }

    private fun listMatches(argument: String, suggest: Iterable<String>): MutableList<String> {
        return suggest.filter { it.contains(argument) }.toMutableList()
    }

    private fun permissionMessage(sender: CommandSender, permission: String): Boolean {
        if (sender.hasPermission(permission)) {
            return true
        }
        sender.sendMessage(
            TextProcess.replace(
                MessageYAMLStorage.fileConfiguration.getString("permission.no-permission")!!,
                permission
            )
        )
        return false
    }

    private fun suggestion(argument: String, suggest: String, sender: CommandSender): Boolean {
        if (argument.equals(suggest, ignoreCase = true)) {
            return permissionMessage(
                sender,
                "${ResidenceStorageSpigotMain.instance.description.name.lowercase()}.command.$suggest"
            )
        }
        return false
    }


    private fun conflictNames(list1: List<String>, list2: List<String>): List<String> {
        val duplicates = mutableListOf<String>()
        val set1 = list1.toSet()
        val set2 = list2.toSet()

        for (element in set1) {
            if (element in set2) {
                duplicates.add(element)
            }
        }

        return duplicates
    }
}