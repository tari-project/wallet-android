package com.tari.android.wallet.ui.screen.settings.backup.verifySeedPhrase

import java.util.*

class SelectionSequence(
    private val original: SeedPhrase,
    private val shuffled: SeedPhrase
) {
    private val selections = LinkedList<Int>()
    val size: Int
        get() = selections.size
    val currentSelection: List<Pair<Int, String>>
        get() = selections.map { Pair(it, shuffled[it]) }
    val isEmpty: Boolean
        get() = selections.isEmpty()
    val isComplete: Boolean
        get() = selections.size == shuffled.length

    init {
        if (original.length != shuffled.length) {
            throw IllegalArgumentException(
                "Original and shuffled phrases' lengths aren't equal." +
                        "\nOriginal: $original\nShuffled: $shuffled"
            )
        }
    }

    fun add(index: Int) {
        if (size == original.length) throw IllegalArgumentException(
            "Selection sequence does already have the necessary size" +
                    "\nTried to add: (index=$index, phrase=$original,shuffled=$shuffled)"
        )
        if (index < 0 || index >= original.length)
            throw IllegalArgumentException(
                "Selection index ($index) is invalid (either negative or bigger than phrase " +
                        "length)\nPhrase: ${original.length}"
            )
        if (selections.indexOfFirst { it == index } != -1)
            throw IllegalArgumentException("This selection ($index) is already present: $this")
        selections.add(index)
    }

    fun remove(index: Int) {
        selections
            .indexOfFirst { it == index }
            .takeIf { it != -1 }
            ?.also { selections.removeAt(it) }
            ?: throw IllegalArgumentException("Index $index is not added. $this")
    }

    fun matchesOriginalPhrase(): Boolean {
        val result = currentSelection
        if (result.size != original.length) throw IllegalStateException(
            "Current selection length (${result.size}) does not match target length " +
                    "(${original.length}).\nCurrent sequence: ${result.joinToString()}" +
                    "\nPhrase to be matched against: $original"
        )
        return original.consistsOf(result.map(Pair<Int, String>::second))
    }

    fun contains(index: Int): Boolean = selections.contains(index)

    override fun toString(): String =
        "SelectionSequence(original=$original, shuffled=$shuffled, selections=$selections)"

}