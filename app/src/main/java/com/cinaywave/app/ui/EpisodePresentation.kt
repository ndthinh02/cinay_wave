package com.cinaywave.app.ui

internal fun episodeTitle(name: String, english: Boolean): String {
    val clean = name.trim()
    if (clean.equals("full", true)) return if (english) "Full movie" else "Trọn bộ"
    if (Regex("^(tập|tap|episode|ep)\\b", RegexOption.IGNORE_CASE).containsMatchIn(clean)) return clean
    return (if (english) "Episode " else "Tập ") + clean
}

internal fun episodeGroups(count: Int): List<IntRange> {
    require(count >= 0)
    return (0 until (count + 29) / 30).map { group -> (group * 30 + 1)..minOf((group + 1) * 30, count) }
}
