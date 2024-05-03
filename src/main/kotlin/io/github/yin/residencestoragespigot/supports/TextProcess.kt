package io.github.yin.residencestoragespigot.supports

object TextProcess {
    fun replace(text: String, vararg parameters: String): String {
        var result = text
        parameters.forEachIndexed { index, parameter ->
            result = result.replace("{$index}", parameter)
        }
        return result
    }

    fun replaceList(list: List<String>, vararg parameters: String): List<String> {
        return list.map { replace(it, *parameters) }
    }
}