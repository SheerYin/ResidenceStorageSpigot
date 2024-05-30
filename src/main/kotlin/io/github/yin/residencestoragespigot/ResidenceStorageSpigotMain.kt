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
        const val prefix = "§f[§7领地储存§f] "
        const val pluginChannel = "BungeeCord"

        lateinit var scope: CoroutineScope

        var serverName = ""
        var playerNames = mutableListOf<String>()
    }

    override fun onEnable() {
        instance = this

        scope = CoroutineScope(Dispatchers.IO)

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

        server.consoleSender.sendMessage(prefix + "插件开始加载 " + description.version)
    }

    override fun onDisable() {
        runBlocking {
            try {
                withTimeout(TimeUnit.MINUTES.toMillis(1)) {
                    server.consoleSender.sendMessage(prefix + "正在等待任务完成，最多等待 1 分钟")
                    scope.coroutineContext[Job]?.children?.forEach { it.join() }
                    server.consoleSender.sendMessage(prefix + "任务全部完成")
                }
            } catch (exception: TimeoutCancellationException) {
                server.consoleSender.sendMessage(prefix + "已超时，强制清理所有任务")
                scope.cancel()
            }
        }

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