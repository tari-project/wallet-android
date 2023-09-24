package com.tari.android.wallet.ui.fragment.settings.backup.verifySeedPhrase

import com.tari.android.wallet.util.TariBuild

class SeedPhrase(private val seedWords: List<String>) : Iterable<String> {
    val length
        get() = seedWords.size

    private fun sorted(): SeedPhrase = SeedPhrase(if (TariBuild.MOCKED) seedWords else seedWords.sorted())

    fun consistsOf(result: List<String>): Boolean = seedWords == result

    operator fun get(index: Int): String {
        if (index >= length) throw IllegalArgumentException(
            "Selection index ($index) isn't less than original phrase's length " +
                    "($length)\nPhrase: $this"
        )
        return seedWords[index]
    }

    fun startSelection() = sorted().let { s -> Pair(s, SelectionSequence(this, s)) }

    override fun iterator(): Iterator<String> = seedWords.iterator()

    override fun equals(other: Any?): Boolean =
        this === other || javaClass == other?.javaClass && seedWords == (other as SeedPhrase).seedWords

    override fun hashCode(): Int = seedWords.hashCode()
    override fun toString(): String = "Phrase(words=${seedWords.joinToString()})"

}