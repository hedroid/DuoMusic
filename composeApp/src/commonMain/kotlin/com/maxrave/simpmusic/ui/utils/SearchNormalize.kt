package com.maxrave.simpmusic.ui.utils

/**
 * 客户端过滤搜索的归一与命中判定(全仓统一入口,2026-11-03 从下载管理页收编)。
 *
 * 背景:"明明在列表里却搜不到"的主因是两侧字符级不一致——中文 IME 打出的全角括号/冒号
 * 与库里的半角逐码点不等、YT 标题夹 NBSP、重音字母、查询词带尾随空格,裸 contains 全部落空。
 *
 * 归一规则(闭式映射,commonMain 双端可用,不依赖 java.text):
 * ①全角 ASCII→半角(U+FF01-FF5E 整段减 0xFEE0,覆盖 ！？（）：ＡＢＣ 全部);
 * ②NBSP/全角空格→普通空格、零宽字符剔除;
 * ③trim + 小写。繁简异体(周杰倫/周杰伦)不做——需千字映射表,不值得。
 */
fun normalizeForSearch(text: String): String =
    buildString(text.length) {
        for (ch in text) {
            when (val c = ch.code) {
                0xA0, 0x3000, 0xFEFF -> append(' ')
                in 0x200B..0x200D -> Unit
                in 0xFF01..0xFF5E -> append(c - 0xFEE0)
                else -> append(c)
            }
        }
    }.trim().lowercase()

/** 单字段归一包含:歌单名/艺人名/侧栏标题这类单串过滤用 */
fun normalizedContains(
    text: String,
    query: String,
): Boolean = normalizeForSearch(text).contains(normalizeForSearch(query))

/**
 * 标题+艺人(逐个+联合串)命中判定,下载管理/歌单详情/MV 两页/库页动态列表共用口径:
 * 标题或任一艺人包含即命中——人们找"某歌手的全部歌"不比找单曲少,只搜标题看起来像坏了;
 * 多艺人歌行上显示的是 joinToString 联合串,按显示原文搜"A, B"也要能命中。
 */
fun matchesQuery(
    title: String,
    artists: List<String>?,
    query: String,
): Boolean {
    val q = normalizeForSearch(query)
    if (q.isEmpty()) return true
    if (normalizeForSearch(title).contains(q)) return true
    val list = artists ?: return false
    return list.any { normalizeForSearch(it).contains(q) } ||
        normalizeForSearch(list.joinToString(", ")).contains(q)
}
