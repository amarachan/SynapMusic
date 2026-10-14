package com.synap.music.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SynchronizedLyrics(
    val sentences: List<Pair<Long, String>>,
    private val positionProvider: () -> Long
) {
    var index by mutableStateOf(currentIndex)
        private set

    private val currentIndex: Int
        get() {
            if (sentences.isEmpty()) return 0
            val currentPos = positionProvider()
            var activeIdx = 0
            for (i in sentences.indices) {
                if (sentences[i].first <= currentPos) {
                    activeIdx = i
                } else {
                    break
                }
            }
            return activeIdx
        }

    fun update(): Boolean {
        val newIndex = currentIndex
        return if (newIndex != index) {
            index = newIndex
            true
        } else {
            false
        }
    }
}
