package com.tari.android.wallet.ui.common.gyphy

class GiphyKeywordsRepository {
    private val _words = listOf("money", "money machine", "rich")
    private var _currentIndex = 0

    fun getCurrent() : String = _words[_currentIndex]

    fun getNext() : String {
        _currentIndex++
        if (_currentIndex >= _words.size) {
            _currentIndex = 0
        }
        return getCurrent()
    }
}