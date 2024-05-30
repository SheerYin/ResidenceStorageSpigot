package io.github.yin.residencestoragespigot.storages

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.supports.ResidenceInfo
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.util.*

object ResidenceMySQLStorage {

    private lateinit var path: Path
    fun initialize(file: File) {
        var folder = Paths.get(ConfigurationYAMLStorage.configuration.getString("residence.file.path"))
        if (folder.toString().isEmpty()) {
            folder = Paths.get(file.path, "Residence")
        } else {
            if (folder.startsWith(Paths.get("plugins"))) {
                folder = file.toPath().parent.resolve(folder.subpath(1, folder.nameCount))
            }
        }
        Files.createDirectories(folder)
        path = folder.resolve("mysql.yml")
        if (!Files.exists(path)) {
            val stream = ResidenceStorageSpigotMain.instance.getResource("Residence/mysql.yml")
            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private lateinit var dataSource: HikariDataSource
    private lateinit var tablePrefix: String
    fun load() {
        val configuration = YamlConfiguration.loadConfiguration(path.toFile())

        val config = HikariConfig()
        with(configuration) {
            config.jdbcUrl = getString("mysql.url")
            config.username = getString("mysql.username")
            config.password = getString("mysql.password")
            config.maximumPoolSize = getInt("mysql.maximum-pool-size")
            config.minimumIdle = getInt("mysql.minimum-idle")
            config.connectionTimeout = getLong("mysql.connection-timeout")
            config.idleTimeout = getLong("mysql.idle-timeout")
            config.maxLifetime = getLong("mysql.maximum-lifetime")

            tablePrefix = getString("mysql.table-prefix")!!
        }

        dataSource = HikariDataSource(config)

        createTable()
    }


    private fun getConnection(): Connection {
        return dataSource.connection
    }

    fun close() {
        dataSource.close()
    }

    private lateinit var table: String
    private fun createTable() {
        table = tablePrefix + "residence"
        val sql = "CREATE TABLE IF NOT EXISTS $table (residence_name VARCHAR(64) PRIMARY KEY, owner_uuid VARCHAR(36), owner VARCHAR(64), residence_flags JSON, player_flags JSON, server_name VARCHAR(64));"
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(sql)
            }
        }
    }

    private val gson = Gson()

    fun addResidence(residenceInfo: ResidenceInfo): Boolean {
        val sql = "INSERT IGNORE INTO $table (residence_name, owner_uuid, owner, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?, ?)"
        getConnection().use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceInfo.residenceName)
                preparedStatement.setString(2, residenceInfo.ownerUUID)
                preparedStatement.setString(3, residenceInfo.owner)
                preparedStatement.setString(4, gson.toJson(residenceInfo.residenceFlags))
                preparedStatement.setString(5, gson.toJson(residenceInfo.playerFlags))
                preparedStatement.setString(6, residenceInfo.serverName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


    fun addResidences(residenceInfos: MutableList<ResidenceInfo>): Boolean {
        val sql = "INSERT INTO $table (residence_name, owner_uuid, owner, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?, ?)"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                for (residenceInfo in residenceInfos) {
                    preparedStatement.setString(1, residenceInfo.residenceName)
                    preparedStatement.setString(2, residenceInfo.ownerUUID)
                    preparedStatement.setString(3, residenceInfo.owner)
                    preparedStatement.setString(4, gson.toJson(residenceInfo.residenceFlags))
                    preparedStatement.setString(5, gson.toJson(residenceInfo.playerFlags))
                    preparedStatement.setString(6, residenceInfo.serverName)
                    preparedStatement.addBatch()
                }
                val results = preparedStatement.executeBatch()
                return results.all { it > 0 }
            }
        }
    }


    fun removeResidence(residenceName: String): Boolean {
        val sql = "DELETE FROM $table WHERE residence_name = ? LIMIT 1"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


    fun getResidence(residenceName: String): ResidenceInfo? {
        val sql = "SELECT * FROM $table WHERE residence_name = ? LIMIT 1"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                val resultSet = preparedStatement.executeQuery()
                if (resultSet.next()) {
                    return ResidenceInfo(
                        residenceName,
                        resultSet.getString("owner_uuid"),
                        resultSet.getString("owner"),
                        gson.fromJson(
                            resultSet.getString("residence_flags"),
                            object : TypeToken<MutableMap<String, Boolean>>() {}.type
                        ),
                        gson.fromJson(
                            resultSet.getString("player_flags"),
                            object : TypeToken<MutableMap<String, MutableMap<String, Boolean>>>() {}.type
                        ),
                        resultSet.getString("server_name")
                    )
                }
                return null
            }
        }
    }


    fun getResidences(): List<ResidenceInfo> {
        val list = mutableListOf<ResidenceInfo>()

        val sql = "SELECT * FROM $table"
        getConnection().use { connection: Connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    while (resultSet.next()) {
                        val residenceInfo = ResidenceInfo(
                            resultSet.getString("residence_name"),
                            resultSet.getString("owner_uuid"),
                            resultSet.getString("owner"),
                            gson.fromJson(
                                resultSet.getString("residence_flags"),
                                object : TypeToken<MutableMap<String, Boolean>>() {}.type
                            ),
                            gson.fromJson(
                                resultSet.getString("player_flags"),
                                object : TypeToken<MutableMap<String, MutableMap<String, Boolean>>>() {}.type
                            ),
                            resultSet.getString("server_name")
                        )
                        list.add(residenceInfo)
                    }
                    return list
                }
            }
        }
    }


    fun getResidenceNames(): List<String> {
        val list = mutableListOf<String>()

        val sql = "SELECT residence_name FROM $table"
        getConnection().use { connection: Connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    while (resultSet.next()) {
                        val residenceName = resultSet.getString("residence_name")
                        list.add(residenceName)
                    }
                    return list
                }
            }
        }
    }


    fun getOwnerResidenceNames(ownerUUID: UUID): List<String> {
        val list: MutableList<String> = ArrayList()

        val sql = "SELECT residence_name FROM $table WHERE owner_uuid = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        list.add(resultSet.getString("residence_name"))
                    }
                }

            }
        }
        return list
    }


    fun getOwnerResidenceNames(owner: String): List<String> {
        val list: MutableList<String> = ArrayList()

        val sql = "SELECT residence_name FROM $table WHERE owner = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, owner)
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        list.add(resultSet.getString("residence_name"))
                    }
                }

            }
        }
        return list
    }


    fun getOwnerResidences(ownerUUID: UUID): List<ResidenceInfo> {
        val list: MutableList<ResidenceInfo> = ArrayList()

        val sql = "SELECT residence_name, owner_uuid, owner, residence_flags, player_flags, server_name FROM $table WHERE owner_uuid = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val residenceInfo = ResidenceInfo(
                            resultSet.getString("residence_name"),
                            resultSet.getString("owner_uuid"),
                            resultSet.getString("owner"),
                            gson.fromJson(
                                resultSet.getString("residence_flags"),
                                object : TypeToken<MutableMap<String, Boolean>>() {}.type
                            ),
                            gson.fromJson(
                                resultSet.getString("player_flags"),
                                object : TypeToken<MutableMap<String, MutableMap<String, Boolean>>>() {}.type
                            ),
                            resultSet.getString("server_name")
                        )
                        list.add(residenceInfo)
                    }
                }
            }

        }
        return list
    }

    fun getOwnerResidences(owner: String): List<ResidenceInfo> {
        val list: MutableList<ResidenceInfo> = ArrayList()

        val sql = "SELECT residence_name, owner_uuid, owner, residence_flags, player_flags, server_name FROM $table WHERE owner = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, owner)
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val residenceInfo = ResidenceInfo(
                            resultSet.getString("residence_name"),
                            resultSet.getString("owner_uuid"),
                            resultSet.getString("owner"),
                            gson.fromJson(
                                resultSet.getString("residence_flags"),
                                object : TypeToken<MutableMap<String, Boolean>>() {}.type
                            ),
                            gson.fromJson(
                                resultSet.getString("player_flags"),
                                object : TypeToken<MutableMap<String, MutableMap<String, Boolean>>>() {}.type
                            ),
                            resultSet.getString("server_name")
                        )
                        list.add(residenceInfo)
                    }
                }
            }

        }
        return list
    }

    fun updateResidenceOwner(residenceName: String, ownerUUID: UUID, owner: String): Boolean {
        val sql13 = "UPDATE $table SET uuid = ?, owner = ? WHERE residence_name = ? LIMIT 1"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql13).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.setString(2, owner)
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


    fun updateResidenceName(oldName: String, newName: String): Boolean {
        val sql14 = "UPDATE $table SET residence_name = ? WHERE residence_name = ? LIMIT 1"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql14).use { preparedStatement ->
                preparedStatement.setString(1, newName)
                preparedStatement.setString(2, oldName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun setResidenceFlags(residenceName: String, key: String, value: Boolean): Boolean {
        val sql = "UPDATE $table SET residence_flags = JSON_SET(residence_flags, ?, ?) WHERE residence_name = ? LIMIT 1"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1,"$.\"$key\"")
                preparedStatement.setString(2, value.toString())
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun removeResidenceFlags(residenceName: String, key: String): Boolean {
        val sql = "UPDATE $table SET residence_flags = JSON_REMOVE(residence_flags, ?) WHERE residence_name = ? LIMIT 1"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1,"$.\"$key\"")
                preparedStatement.setString(2, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun setPlayerFlags(residenceName: String, playerUUID: UUID, key: String, value: Boolean): Boolean {
        val sql = "UPDATE $table SET player_flags = JSON_SET(player_flags, ?, ?) WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1,"$.\"$playerUUID\".\"$key\"")
                preparedStatement.setString(2, value.toString())
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0

            }
        }
    }


    fun removePlayerFlags(residenceName: String, playerUUID: UUID, key: String): Boolean {
        val sql = "UPDATE $table SET player_flags = JSON_REMOVE(player_flags, ?) WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1,"$.\"$playerUUID\".\"$key\"")
                preparedStatement.setString(2, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


}