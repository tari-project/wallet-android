package com.tari.android.wallet.model.yat

import android.content.Context
import com.google.gson.Gson
import com.tari.android.wallet.infrastructure.yat.emojiid.YatEmojiIdAPI
import de.adorsys.android.securestoragelibrary.SecurePreferences
import java.util.concurrent.atomic.AtomicReference

// Implementers expected to be thread-safe
interface EmojiSet {
    val set: Set<String>?
}

interface MutableEmojiSet : EmojiSet {
    override var set: Set<String>?
}

interface ActualizingEmojiSet : EmojiSet {
    fun actualize()
}

class EmojiSetActualizationException(message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause)

class AtomicCacheEmojiSetDecorator(private val delegate: MutableEmojiSet) : MutableEmojiSet {

    private val atomicSet = AtomicReference<Set<String>?>()

    override var set: Set<String>?
        get() = atomicSet.get() ?: delegate.set.also(atomicSet::set)
        set(value) {
            delegate.set = value
            atomicSet.set(value)
        }

}

class PersistingEmojiSet(private val context: Context, private val gson: Gson) : MutableEmojiSet {

    @Suppress("UNCHECKED_CAST")
    override var set: Set<String>?
        get() = retrieveEmojiIdParts(context)
            ?.let { gson.fromJson(it.joinToString(""), Set::class.java) as Set<String>? }
        set(value) = if (value == null) SecurePreferences.removeValue(context, KEY_EMOJI_SET)
        else gson.toJson(value).chunked(STRING_CHUNK).toCollection(LinkedHashSet())
            .let { SecurePreferences.setValue(context, KEY_EMOJI_SET, it) }


    // Because we need an ordered list rather than unordered set
    private fun retrieveEmojiIdParts(context: Context): List<String>? {
        val size = SecurePreferences.getIntValue(context, KEY_EMOJI_SET + KEY_SET_COUNT_POSTFIX, -1)
        return if (size == -1) null
        else (0 until size)
            .map { SecurePreferences.getStringValue(context, KEY_EMOJI_SET + "_" + it, "")!! }
    }

    private companion object {
        private const val KEY_EMOJI_SET = "emoji_set"
        private const val KEY_SET_COUNT_POSTFIX = "_count"
        private const val STRING_CHUNK = 150
    }

}

class YatAPISynchronizingSet(
    private val emojiSet: MutableEmojiSet,
    private val api: YatEmojiIdAPI
) : ActualizingEmojiSet {
    override val set: Set<String>?
        get() = emojiSet.set

    override fun actualize() {
        val response = api.getSupportedSet().execute()
        if (response.isSuccessful) {
            emojiSet.set = response.body()!!.toSet()
        } else {
            throw EmojiSetActualizationException(message = response.errorBody()?.string())
        }
    }

}
