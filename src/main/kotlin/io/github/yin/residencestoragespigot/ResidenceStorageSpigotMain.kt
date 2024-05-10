package io.github.yin.residencestoragespigot

import io.github.yin.residencestoragespigot.commands.ResidenceStorageTabExecutor
import io.github.yin.residencestoragespigot.listeners.ReceivePluginMessage
import io.github.yin.residencestoragespigot.listeners.residence.*
import io.github.yin.residencestoragespigot.storages.ConfigurationYAMLStorage
import io.github.yin.residencestoragespigot.storages.MessageYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class ResidenceStorageSpigotMain : JavaPlugin() {

    companion object {
        lateinit var instance: ResidenceStorageSpigotMain
        const val prefix = "§f[§7领地储存§f] "
        const val pluginChannel = "residencestorage:channel"
    }

    override fun onEnable() {
        instance = this
        server.consoleSender.sendMessage(prefix + "插件开始加载 " + description.version)

        ConfigurationYAMLStorage.initialization(dataFolder)
        ConfigurationYAMLStorage.load()
        val fileConfiguration = ConfigurationYAMLStorage.fileConfiguration
        MessageYAMLStorage.initialization(dataFolder)
        MessageYAMLStorage.load()

        val mysql = ResidenceMySQLStorage.Parameter(
            fileConfiguration.getString("residences.mysql.url")!!,
            fileConfiguration.getString("residences.mysql.username")!!,
            fileConfiguration.getString("residences.mysql.password")!!,
            fileConfiguration.getInt("residences.mysql.maximum-pool-size"),
            fileConfiguration.getInt("residences.mysql.minimum-idle"),
            fileConfiguration.getLong("residences.mysql.connection-timeout"),
            fileConfiguration.getLong("residences.mysql.idle-timeout"),
            fileConfiguration.getLong("residences.mysql.maximum-lifetime")
        )
        ResidenceMySQLStorage.initialization(mysql, fileConfiguration.getString("residences.mysql.table-prefix")!!)
        ResidenceMySQLStorage.createTable()

        server.messenger.registerOutgoingPluginChannel(this, pluginChannel)
        server.messenger.registerIncomingPluginChannel(this, pluginChannel, ReceivePluginMessage)

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


}