package io.github.yin.residencestoragespigot.storages

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.yin.residencestoragespigot.supports.ResidenceInfo
import java.sql.Connection


object ResidenceMySQLStorage {

    class Parameter(
        val url: String,
        val username: String,
        val password: String,
        val maximumPoolSize: Int,
        val minimumIdle: Int,
        val connectionTimeout: Long,
        val idleTimeout: Long,
        val maxLifetime: Long
    )

    private lateinit var dataSource: HikariDataSource
    private lateinit var tablePrefix: String

    fun initialization(parameter: Parameter, tablePrefix: String) {
        val config = HikariConfig()
        config.jdbcUrl = parameter.url
        config.username = parameter.username
        config.password = parameter.password
        config.maximumPoolSize = parameter.maximumPoolSize
        config.minimumIdle = parameter.minimumIdle
        config.connectionTimeout = parameter.connectionTimeout
        config.idleTimeout = parameter.idleTimeout
        config.maxLifetime = parameter.maxLifetime

        config.transactionIsolation = "TRANSACTION_SERIALIZABLE"
        dataSource = HikariDataSource(config)

        this.tablePrefix = tablePrefix
    }

    private fun getConnection(): Connection {
        return dataSource.connection
    }

    fun close() {
        dataSource.close()
    }

    fun createTable() {
        val table = tablePrefix + "residences"
        val sql = """
                CREATE TABLE IF NOT EXISTS $table (
                residence_name VARCHAR(64) PRIMARY KEY,
                owner_uuid VARCHAR(64),
                owner VARCHAR(64),
                residence_flags JSON,
                player_flags JSON,
                server_name VARCHAR(64)
                );
                """.trimIndent()
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(sql)
            }
        }
    }

    private val gson = Gson()
    fun addResidence(residenceInfo: ResidenceInfo): Boolean {
        val table = tablePrefix + "residences"
        val sql =
            "INSERT INTO $table (residence_name, owner_uuid, owner, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?, ?)"
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
        val table = tablePrefix + "residences"
        val sql =
            "INSERT INTO $table (residence_name, owner_uuid, owner, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?, ?)"
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
        val table = tablePrefix + "residences"
        val sql = "DELETE FROM $table WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


    fun getResidence(residenceName: String): ResidenceInfo? {
        var residenceInfo: ResidenceInfo? = null
        val table = tablePrefix + "residences"
        val sql = "SELECT * FROM $table WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
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
                preparedStatement.connection.close()
            }
        }
        return residenceInfo
    }

    fun getResidences(): MutableList<ResidenceInfo> {
        val list = ArrayList<ResidenceInfo>()

        val table = tablePrefix + "residences"
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
                }
            }
        }
        return list
    }

    fun getResidenceNames(): MutableList<String> {
        val list = mutableListOf<String>()

        val table = tablePrefix + "residences"
        val sql = "SELECT residence_name FROM $table"
        getConnection().use { connection: Connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    while (resultSet.next()) {
                        val residenceName = resultSet.getString("residence_name")
                        list.add(residenceName)
                    }
                }
            }
        }

        return list
    }

    fun hasResidenceName(residenceName: String): Boolean {
        val table = tablePrefix + "residences"
        val sql = "SELECT residence_name FROM $table WHERE residence_name LIKE ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                val resultSet = preparedStatement.executeQuery()
                return resultSet.next()
            }
        }
    }

    fun hasOwnerUUIDResidenceName(ownerUUID: String, residenceName: String): Boolean {
        val table = tablePrefix + "residences"
        val sql = "SELECT EXISTS (SELECT 1 FROM $table WHERE owner_uuid = ? AND residence_name = ?)"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID)
                preparedStatement.setString(2, residenceName)
                preparedStatement.executeQuery().use { resultSet ->
                    resultSet.next().let { return resultSet.getBoolean(1) }
                }
            }
        }
    }

    fun getOwnerUUIDResidenceNames(ownerUUID: String): MutableList<String> {
        val table = tablePrefix + "residences"
        val sql = "SELECT residence_name FROM $table WHERE owner_uuid = ?"
        val list: MutableList<String> = ArrayList()
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID)
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        list.add(resultSet.getString("residence_name"))
                    }
                }

            }
        }
        return list
    }

    fun getOwnerUUIDResidences(ownerUUID: String): List<ResidenceInfo> {
        val list: MutableList<ResidenceInfo> = ArrayList()
        val table = tablePrefix + "residences"
        val sql =
            "SELECT residence_name, owner_uuid, owner, residence_flags, player_flags, server_name FROM $table WHERE owner_uuid = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID)
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

    fun changeOwner(residenceName: String, ownerUUID: String, owner: String): Boolean {
        val table = tablePrefix + "residences"
        val sql = "UPDATE $table SET uuid = ?, owner = ? WHERE residence_name = ?"

        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID)
                preparedStatement.setString(2, owner)
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun changeName(oldName: String, newName: String): Boolean {
        val table = tablePrefix + "residences"
        val sql = "UPDATE $table SET residence_name = ? WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, newName)
                preparedStatement.setString(2, oldName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun setResidenceFlags(residenceName: String, key: String, value: Boolean): Boolean {
        if (!key.matches("[a-zA-Z0-9]+".toRegex())) {
            return false
        }

        val table = tablePrefix + "residences"
        val sql = "UPDATE $table SET residence_flags = JSON_SET(residence_flags, '$.$key', ?) WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, value.toString())
                preparedStatement.setString(2, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun removeResidenceFlags(residenceName: String, key: String): Boolean {
        if (!key.matches("[a-zA-Z0-9]+".toRegex())) {
            return false
        }

        val table = tablePrefix + "residences"
        val sql = "UPDATE $table SET residence_flags = JSON_REMOVE(residence_flags, '$.$key') WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun setPlayerFlags(residenceName: String, playerUUID: String, key: String, value: Boolean): Boolean {
        if (!playerUUID.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))) {
            return false
        }
        if (!key.matches(Regex("[a-zA-Z0-9]+"))) {
            return false
        }
        val table = tablePrefix + "residences"
        val sql =
            "UPDATE $table SET player_flags = JSON_SET(player_flags, '$.$playerUUID.$key', ?) WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, value.toString())
                preparedStatement.setString(2, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun removePlayerFlags(residenceName: String, playerUUID: String, key: String): Boolean {
        if (!playerUUID.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))) {
            return false
        }
        if (!key.matches(Regex("[a-zA-Z0-9]+"))) {
            return false
        }

        val table = tablePrefix + "residences"
        val sql =
            "UPDATE $table SET player_flags = JSON_REMOVE(player_flags, '$.$playerUUID.$key') WHERE residence_name = ?"
        getConnection().use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


}