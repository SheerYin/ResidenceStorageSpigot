package io.github.yin.residencestoragespigot

import io.github.yin.residencestoragespigot.commands.ResidenceStorageTabExecutor
import io.github.yin.residencestoragespigot.listeners.PlayerJoin
import io.github.yin.residencestoragespigot.listeners.ReceivePluginMessage
import io.github.yin.residencestoragespigot.listeners.residence.*
import io.github.yin.residencestoragespigot.storages.ConfigurationYAMLStorage
import io.github.yin.residencestoragespigot.storages.MessageYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class ResidenceStorageSpigotMain : JavaPlugin() {

    companion object {
        lateinit var instance: ResidenceStorageSpigotMain
        const val prefix = "§f[§7领地储存§f] "
        const val pluginChannel = "BungeeCord"

        var serverName = ""
        var playerNames = mutableListOf<String>()
    }

    override fun onEnable() {
        instance = this
        server.consoleSender.sendMessage(prefix + "插件开始加载 " + description.version)

        ConfigurationYAMLStorage.initialize(dataFolder)
        ConfigurationYAMLStorage.load()
        MessageYAMLStorage.initialize(dataFolder)
        MessageYAMLStorage.load()
        ResidenceMySQLStorage.initialize(dataFolder)
        ResidenceMySQLStorage.load()

        server.messenger.registerOutgoingPluginChannel(this, pluginChannel)
        server.messenger.registerIncomingPluginChannel(this, pluginChannel, ReceivePluginMessage)

        Bukkit.getPluginManager().registerEvents(PlayerJoin, this)

        Bukkit.getPluginManager().registerEvents(ResidenceCommand, this)
        Bukkit.getPluginManager().registerEvents(ResidenceCreation, this)
        Bukkit.getPluginManager().registerEvents(ResidenceDelete, this)
        Bukkit.getPluginManager().registerEvents(ResidenceFlagChange, this)
        Bukkit.getPluginManager().registerEvents(ResidenceOwnerChange, this)
        Bukkit.getPluginManager().registerEvents(ResidenceRename, this)

        getCommand("residencestoragespigot")?.setExecutor(ResidenceStorageTabExecutor)
    }

    override fun onDisable() {
        ResidenceMySQLStorage.close()
        server.consoleSender.sendMessage(prefix + "插件开始卸载 " + description.version)
    }

    fun sendByteServerName(player: Player) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).use { output ->
            output.writeUTF("GetServer")
        }
        player.sendPluginMessage(this, pluginChannel, byteArrayOutputStream.toByteArray())
    }

    fun sendBytePlayerNames(player: Player) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).use { output ->
            output.writeUTF("PlayerList")
            output.writeUTF("ALL")
        }
        player.sendPluginMessage(this, pluginChannel, byteArrayOutputStream.toByteArray())
    }


}