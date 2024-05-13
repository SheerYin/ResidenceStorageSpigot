package io.github.yin.residencestoragespigot.supports

import java.time.Duration
import java.time.Instant

object Cooldown {

    private var last: Instant = Instant.now()

    fun globalUse(cooldown: Duration): Boolean {
        val now = Instant.now()
        if (Duration.between(last, now).toMillis() >= cooldown.toMillis()) {
            last = now
            return true
        }
        return false
    }


}