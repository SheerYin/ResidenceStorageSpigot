package io.github.yin.residencestoragespigot.supports

object IndexReplace {

    /**
     * 用给定的参数替换文本中的占位符，没有副作用。
     *
     * @param text 包含占位符的原始文本。占位符的格式为 {index}，其中 index 是参数在参数列表中的位置。
     * @param parameters 用于替换占位符的参数。参数的位置决定了它将替换哪个占位符。
     * @return 替换了占位符的文本。
     */
    fun replace(text: String, vararg parameters: String): String {
        var result = text
        parameters.forEachIndexed { index, parameter ->
            result = result.replace("{$index}", parameter)
        }
        return result
    }

    /**
     * 用给定的参数替换列表中每个文本的占位符，没有副作用。
     *
     * @param list 包含占位符的文本列表。
     * @param parameters 用于替换占位符的参数。
     * @return 替换了占位符的文本列表。
     */
    fun replaceList(list: List<String>, vararg parameters: String): List<String> {
        return list.map { replace(it, *parameters) }
    }
}