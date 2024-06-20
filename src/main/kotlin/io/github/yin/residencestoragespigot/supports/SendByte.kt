package io.github.yin.residencestoragespigot.supports

import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

object SendByte {
    fun serverName(player: Player) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).use { output ->
            output.writeUTF("GetServer")
        }
        player.sendPluginMessage(ResidenceStorageSpigotMain.instance, ResidenceStorageSpigotMain.pluginChannel, byteArrayOutputStream.toByteArray())
    }

    fun playerNames(player: Player) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).use { output ->
            output.writeUTF("PlayerList")
            output.writeUTF("ALL")
        }
        player.sendPluginMessage(ResidenceStorageSpigotMain.instance, ResidenceStorageSpigotMain.pluginChannel, byteArrayOutputStream.toByteArray())
    }
}