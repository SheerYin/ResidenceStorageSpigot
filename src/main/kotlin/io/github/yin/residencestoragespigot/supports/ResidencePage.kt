package io.github.yin.residencestoragespigot.supports

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object ResidencePage {
    val playerPage = ConcurrentHashMap<String, CopyOnWriteArrayList<CopyOnWriteArrayList<String>>>()
    var allPage = CopyOnWriteArrayList<ConcurrentHashMap<String, ResidenceInfo>>()

    fun split(original: List<String>, pageSize: Int): CopyOnWriteArrayList<CopyOnWriteArrayList<String>> {
        if (original.isEmpty()) {
            return CopyOnWriteArrayList()
        }
        val split = mutableListOf<CopyOnWriteArrayList<String>>()
        var index = 0
        while (index < original.size) {
            split.add(CopyOnWriteArrayList(original.subList(index, (index + pageSize).coerceAtMost(original.size))))
            index += pageSize
        }
        return CopyOnWriteArrayList(split)
    }


    fun allSplit(
        map: Map<String, ResidenceInfo>,
        pageSize: Int
    ): CopyOnWriteArrayList<ConcurrentHashMap<String, ResidenceInfo>> {

        val entries = map.entries.toList()
        val list = CopyOnWriteArrayList<ConcurrentHashMap<String, ResidenceInfo>>()

        entries.chunked(pageSize).forEach { chunk ->
            val pageMap = ConcurrentHashMap<String, ResidenceInfo>()
            chunk.forEach { entry ->
                pageMap[entry.key] = entry.value
            }
            list.add(pageMap)
        }
        return list
    }

}