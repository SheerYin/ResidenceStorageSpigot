package io.github.yin.residencestoragespigot

import io.github.yin.residencestoragespigot.commands.ResidenceStorageTabExecutor
import io.github.yin.residencestoragespigot.listeners.PlayerJoin
import io.github.yin.residencestoragespigot.listeners.ReceivePluginMessage
import io.github.yin.residencestoragespigot.listeners.residence.*
import io.github.yin.residencestoragespigot.storages.ConfigurationYAMLStorage
import io.github.yin.residencestoragespigot.storages.MessageYAMLStorage
import io.github.yin.residencestoragespigot.storages.ResidenceMySQLStorage
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.TimeUnit

class ResidenceStorageSpigotMain : JavaPlugin() {

    companion object {
        lateinit var instance: ResidenceStorageSpigotMain

        lateinit var pluginName: String
        lateinit var lowercaseName: String
        lateinit var pluginVersion: String
        lateinit var pluginAuthors: List<String>
        lateinit var pluginPrefix: String

        const val pluginChannel = "BungeeCord"

        lateinit var scope: CoroutineScope

        var hookPlaceholderAPI = false

        var serverName = ""
        var playerNames = mutableListOf<String>()
    }

    override fun onEnable() {
        instance = this
        pluginName = description.name
        lowercaseName = pluginName.lowercase()
        pluginVersion = description.version
        pluginAuthors = description.authors
        pluginPrefix = "§f[§7${description.prefix}§f] "

        server.consoleSender.sendMessage(pluginPrefix + "插件开始加载 " + pluginVersion)

        scope = CoroutineScope(Dispatchers.IO)

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            hookPlaceholderAPI = true
            // BatchCommandExpansion(this).register()
        } else {
            server.consoleSender.sendMessage(pluginPrefix + "没有找到 PlaceholderAPI 无法提供解析 PlaceholderAPI 变量")
        }

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
        server.consoleSender.sendMessage(pluginPrefix + "插件开始卸载 " + pluginVersion)

        runBlocking {
            try {
                withTimeout(TimeUnit.MINUTES.toMillis(1)) {
                    server.consoleSender.sendMessage(pluginPrefix + "正在等待任务完成，最多等待 1 分钟")
                    scope.coroutineContext[Job]?.children?.forEach { it.join() }
                    server.consoleSender.sendMessage(pluginPrefix + "任务全部完成")
                }
            } catch (exception: TimeoutCancellationException) {
                server.consoleSender.sendMessage(pluginPrefix + "已超时，强制清理所有任务")
                scope.cancel()
            }
        }

        ResidenceMySQLStorage.close()
    }


}