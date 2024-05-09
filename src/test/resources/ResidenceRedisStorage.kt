import com.google.gson.Gson
import io.github.yin.residencestoragespigot.Main
import io.github.yin.residencestoragespigot.supports.ResidenceInfo
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.ScanParams
import redis.clients.jedis.params.SetParams
import java.time.Duration
import java.util.*


object ResidenceRedisStorage {

    class Parameter(
        val url: String,
        val maximumTotal: Int,
        val maximumIdle: Int,
        val minimumIdle: Int,
        val maximumWait: Long,
    )


    private lateinit var jedisPool: JedisPool
    private lateinit var channel: String
    private lateinit var prefixKey: String
    private lateinit var name: String
    private lateinit var task: BukkitTask

    fun initialization(parameter: Parameter, channel: String, prefixKey: String) {
        val poolConfig = JedisPoolConfig()
        poolConfig.maxTotal = parameter.maximumTotal
        poolConfig.maxIdle = parameter.maximumIdle
        poolConfig.minIdle = parameter.minimumIdle
        poolConfig.setMaxWait(Duration.ofMillis(parameter.maximumWait))
        jedisPool = JedisPool(poolConfig, parameter.url)

        this.channel = channel
        this.prefixKey = prefixKey
        name = UUID.randomUUID().toString()

        getResource().use { jedis ->
            jedis.set(prefixKey + "connection:" + name, Main.serverName, SetParams().ex(60))
        }

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.instance, Runnable {
            getResource().use { jedis ->
                jedis.set(prefixKey + "connection:" + name, Main.serverName, SetParams().ex(60))

            }
        }, 1200, 0)
    }

    private fun getResource(): Jedis {
        return jedisPool.resource
    }

    fun close() {
        jedisPool.close()
    }

    fun clear() {
        task.cancel()
        getResource().use { jedis ->
            jedis.del(prefixKey + "connection:" + name)
            var isEmpty = true
            lateinit var cursor: String
            do {
                val scan = jedis.scan(ScanParams.SCAN_POINTER_START, ScanParams().match(prefixKey + "connection:*"))
                val keys = scan.result
                if (keys.isNotEmpty()) {
                    isEmpty = false
                    break
                }
                cursor = scan.cursor
            } while (cursor != ScanParams.SCAN_POINTER_START)

            if (isEmpty) {
                val lockKey = prefixKey + "manage:lock"
                val lockValue = UUID.randomUUID().toString()
                val key: String? = jedis.set(lockKey, lockValue, SetParams().nx().ex(10))
                if (key != null && key.equals("OK", ignoreCase = true)) {
                    val residenceNames: MutableList<String> = mutableListOf()
                    do {
                        val scan = jedis.scan(ScanParams.SCAN_POINTER_START, ScanParams().match(prefixKey + "info:*"))
                        val keys = scan.result
                        if (keys.isNotEmpty()) {
                            residenceNames.addAll(keys)
                        }
                        cursor = scan.cursor
                    } while (cursor != ScanParams.SCAN_POINTER_START)
                    jedis.pipelined().use { pipeline ->
                        for (residenceName in residenceNames) {
                            pipeline.del(residenceName)
                        }
                        pipeline.sync()
                    }
                    val luaScript =
                        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
                    jedis.eval(luaScript, 1, lockKey, lockValue)
                }
            }
        }
    }

    private val gson = Gson()

    fun load(map: MutableMap<String, ResidenceInfo>) {
        getResource().use { jedis ->
            val lockKey = prefixKey + "manage:lock"
            val lockValue = UUID.randomUUID().toString()
            for (retry in 1..10) {
                val key: String? = jedis.set(lockKey, lockValue, SetParams().nx().ex(10))
                if (key != null && key.equals("OK", ignoreCase = true)) {
                    var isEmpty = true
                    lateinit var cursor: String
                    do {
                        val scan = jedis.scan(ScanParams.SCAN_POINTER_START, ScanParams().match(prefixKey + "info:*"))
                        val keys = scan.result
                        if (keys.isNotEmpty()) {
                            isEmpty = false
                        }
                        cursor = scan.cursor
                    } while (cursor != ScanParams.SCAN_POINTER_START)
                    if (isEmpty) {
                        val pipeline = jedis.pipelined()
                        for (entry in map) {
                            pipeline.set(prefixKey + "info:" + entry.key, gson.toJson(entry.value))
                        }
                        pipeline.sync()
                        break
                    } else {
                        return
                    }
                } else {
                    Thread.sleep(1000)
                }
            }
            val luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
            jedis.eval(luaScript, 1, lockKey, lockValue)
        }
    }

    fun setResidence(residenceInfo: ResidenceInfo): Boolean {
        val lockKey = prefixKey + "info:lock:" + residenceInfo.residenceName
        val lockValue = UUID.randomUUID().toString()
        getResource().use { jedis ->
            for (retry in 1..10) {
                val key: String? = jedis.set(lockKey, lockValue, SetParams().nx().ex(1))
                if (key != null && key.equals("OK", ignoreCase = true)) {
                    jedis.set(prefixKey + "info:" + residenceInfo.residenceName, gson.toJson(residenceInfo))
                    break
                } else {
                    if (retry == 10) {
                        return false
                    }
                    Thread.sleep(100)
                }
            }
            val luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
            jedis.eval(luaScript, 1, lockKey, lockValue)
        }
        return true
    }

}