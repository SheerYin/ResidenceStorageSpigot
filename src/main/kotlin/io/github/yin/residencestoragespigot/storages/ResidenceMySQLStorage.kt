package io.github.yin.residencestoragespigot.storages

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.yin.residencestoragespigot.ResidenceStorageSpigotMain
import io.github.yin.residencestoragespigot.supports.ResidenceInfo
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
        var folder = Paths.get(ConfigurationYAMLStorage.configuration.getString("economy.file.path"))
        if (folder.toString().isEmpty()) {
            folder = Paths.get(file.path, "Storage")
        } else {
            if (folder.startsWith(Paths.get("plugins"))) {
                folder = file.toPath().parent.resolve(folder.subpath(1, folder.nameCount))
            }
        }
        Files.createDirectories(folder)
        path = folder.resolve("mysql.yml")
        if (!Files.exists(path)) {
            val stream = ResidenceStorageSpigotMain.instance.getResource("Storage/mysql.yml")
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

    private val table = tablePrefix + "residences"
    private val createTableSql = "CREATE TABLE IF NOT EXISTS $table (residence_name VARCHAR(64) PRIMARY KEY, owner_uuid VARCHAR(32), owner VARCHAR(64), residence_flags JSON, player_flags JSON, server_name VARCHAR(64));"
    fun createTable() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(createTableSql)
            }
        }
    }

    private val gson = Gson()
    private val addSql = "INSERT IGNORE INTO $table (residence_name, owner_uuid, owner, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?, ?)"
    fun addResidence(residenceInfo: ResidenceInfo): Boolean {
        getConnection().use { connection ->
            connection.prepareStatement(addSql).use { preparedStatement ->
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

    private val sql2 = "INSERT INTO $table (residence_name, owner_uuid, owner, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?, ?)"
    fun addResidences(residenceInfos: MutableList<ResidenceInfo>): Boolean {
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql2).use { preparedStatement ->
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

    private val sq3 = "DELETE FROM $table WHERE residence_name = ? LIMIT 1"
    fun removeResidence(residenceName: String): Boolean {
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sq3).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    private val sql4 = "SELECT * FROM $table WHERE residence_name = ? LIMIT 1"
    fun getResidence(residenceName: String): ResidenceInfo? {
        var residenceInfo: ResidenceInfo? = null
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql4).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                val resultSet = preparedStatement.executeQuery()
                if (resultSet.next()) {
                    residenceInfo = ResidenceInfo(
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
            }
        }
        return residenceInfo
    }

    private val sql5 = "SELECT * FROM $table"
    fun getResidences(): List<ResidenceInfo> {
        val list = mutableListOf<ResidenceInfo>()
        getConnection().use { connection: Connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql5).use { resultSet ->
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

    private val sql6 = "SELECT residence_name FROM $table"
    fun getResidenceNames(): List<String> {
        val list = mutableListOf<String>()
        getConnection().use { connection: Connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql6).use { resultSet ->
                    while (resultSet.next()) {
                        val residenceName = resultSet.getString("residence_name")
                        list.add(residenceName)
                    }
                    return list
                }
            }
        }
    }

    private val sql7 = "SELECT residence_name FROM $table WHERE residence_name LIKE ? LIMIT 1"
    fun hasResidenceName(residenceName: String): Boolean {
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql7).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.next()
            }
        }
    }

    private val sql8 = "SELECT EXISTS (SELECT 1 FROM $table WHERE owner_uuid = ? AND residence_name = ?) LIMIT 1"
    fun hasOwnerResidenceName(ownerUUID: UUID, residenceName: String): Boolean {
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql8).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.setString(2, residenceName)
                preparedStatement.executeQuery().use { resultSet ->
                    resultSet.next().let { return resultSet.getBoolean(1) }
                }
            }
        }
    }

    private val sql9 = "SELECT residence_name FROM $table WHERE owner_uuid = ?"
    fun getOwnerResidenceNames(ownerUUID: UUID): List<String> {
        val list: MutableList<String> = ArrayList()
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql9).use { preparedStatement ->
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

    private val sql10 = "SELECT residence_name FROM $table WHERE owner = ?"
    fun getOwnerResidenceNames(owner: String): List<String> {
        val list: MutableList<String> = ArrayList()
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql10).use { preparedStatement ->
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

    private val sql11 = "SELECT residence_name, owner_uuid, owner, residence_flags, player_flags, server_name FROM $table WHERE owner_uuid = ?"
    fun getOwnerResidences(ownerUUID: UUID): List<ResidenceInfo> {
        val list: MutableList<ResidenceInfo> = ArrayList()
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql11).use { preparedStatement ->
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

    private val sql12 = "SELECT residence_name, owner_uuid, owner, residence_flags, player_flags, server_name FROM $table WHERE owner = ?"
    fun getOwnerResidences(owner: String): List<ResidenceInfo> {
        val list: MutableList<ResidenceInfo> = ArrayList()
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql12).use { preparedStatement ->
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

    private val sql13 = "UPDATE $table SET uuid = ?, owner = ? WHERE residence_name = ?  LIMIT 1"
    fun updateResidenceOwner(residenceName: String, ownerUUID: UUID, owner: String): Boolean {
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql13).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.setString(2, owner)
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    private val sql14 = "UPDATE $table SET residence_name = ? WHERE residence_name = ?  LIMIT 1"
    fun updateResidenceName(oldName: String, newName: String): Boolean {
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
        val sql = "UPDATE $table SET player_flags = JSON_SET(player_flags, ?, ?) WHERE residence_name = ? LIMIT 1"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1,"$.\"$playerUUID.$key\"")
                preparedStatement.setString(2, value.toString())
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


    fun removePlayerFlags(residenceName: String, playerUUID: UUID, key: String): Boolean {
        val sql = "UPDATE $table SET player_flags = JSON_REMOVE(player_flags, ?) WHERE residence_name = ? LIMIT 1"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1,"$.\"$playerUUID.$key\"")
                preparedStatement.setString(2, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


}