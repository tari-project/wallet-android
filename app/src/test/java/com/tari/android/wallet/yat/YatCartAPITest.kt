/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.yat

import com.tari.android.wallet.infrastructure.yat.YatAPI
import com.tari.android.wallet.infrastructure.yat.YatAPIErrorDTO
import com.tari.android.wallet.infrastructure.yat.cart.CartDTO
import com.tari.android.wallet.infrastructure.yat.cart.CartItemDTO
import com.tari.android.wallet.infrastructure.yat.cart.CartItemsDTO
import com.tari.android.wallet.infrastructure.yat.cart.CheckoutRequestBody
import com.tari.android.wallet.model.yat.EmojiId
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("Should not be ran against a real API, so disabled by default")
class YatCartAPITest {

    private val gateway
        get() = YatAPITestResources.cartGateway
    private val gson
        get() = YatAPITestResources.gson

    @Before
    fun setup() {
        val cleanResponse = gateway.clean().execute()
        check(cleanResponse.isSuccessful) { cleanResponse.errorBody()!!.string() }
    }

    @Test
    fun `cart is empty initially`() {
        val cart = gateway.get().execute().body()!!
        assertTrue(cart.orderItems.isEmpty())
    }

    @Test
    fun `cart was populated after adding a single item to it`() {
        val eid = YatAPI.randomEmojiId(size = 4, set = YatAPITestResources.yatEmojiSet)
            .noVariationSelectors().raw
        val cart: CartDTO = gateway.add(CartItemsDTO(listOf(CartItemDTO(eid)))).execute().body()!!
        // It might be two if we are using a promo code
        assertThat(cart.orderItems.size, anyOf(equalTo(1), equalTo(2)))
        assertEquals(
            eid,
            EmojiId(cart.orderItems.first { it.emojiId != null }.emojiId!!).noVariationSelectors().raw
        )
    }

    @Test
    fun `cart's content was replaced completely after calling replace on cart with 2 items`() {
        val response = gateway.add(
            CartItemsDTO(
                listOf(
                    CartItemDTO(
                        YatAPI.randomEmojiId(
                            size = 4,
                            set = YatAPITestResources.yatEmojiSet
                        ).noVariationSelectors().raw
                    ),
                    CartItemDTO(
                        YatAPI.randomEmojiId(
                            size = 4,
                            set = YatAPITestResources.yatEmojiSet
                        ).noVariationSelectors().raw
                    )
                )
            )
        ).execute()
        assertTrue(response.errorBody()?.string() ?: "Unknown error", response.isSuccessful)
        val eid = YatAPI.randomEmojiId(size = 4, set = YatAPITestResources.yatEmojiSet)
            .noVariationSelectors().raw
        val cart: CartDTO =
            gateway.replace(CartItemsDTO(listOf(CartItemDTO(eid)))).execute().body()!!
        // It might be two if we are using a promo code
        assertThat(cart.orderItems.size, anyOf(equalTo(1), equalTo(2)))
        assertEquals(
            eid,
            EmojiId(cart.orderItems.first { it.emojiId != null }.emojiId!!).noVariationSelectors().raw
        )
    }

    @Test
    fun `cart got empty after clearing a non-empty one`() {
        val eid = YatAPI.randomEmojiId(size = 4, set = YatAPITestResources.yatEmojiSet)
            .noVariationSelectors().raw
        val items = CartItemsDTO(listOf(CartItemDTO(eid)))
        val response = gateway.add(items).execute()
        assertTrue(response.errorBody()?.string(), response.isSuccessful)
        val body = gateway.clean().execute().body()!!
        assertTrue(body.orderItems.isEmpty())
    }

    @Test
    fun `same eid can't be added twice`() {
        val eid = YatAPI.randomEmojiId(size = 4, set = YatAPITestResources.yatEmojiSet)
            .noVariationSelectors().raw
        val items = CartItemsDTO(listOf(CartItemDTO(eid)))
        gateway.add(items).execute().run { check(isSuccessful) { errorBody()!!.string() } }
        val response = gateway.add(items).execute()
        assertFalse(response.errorBody()?.string(), response.isSuccessful)
        val errorBody = gson.fromJson(response.errorBody()!!.string(), YatAPIErrorDTO::class.java)
        assertTrue(errorBody.field("eid").any { it["code"] == ERROR_CODE_ELIGIBILITY })
    }

    @Test
    fun `could purchase an emoji id`() {
        val eid = YatAPI.randomEmojiId(size = 6, set = YatAPITestResources.yatEmojiSet)
            .noVariationSelectors().raw
        val items = CartItemsDTO(listOf(CartItemDTO(eid)))
        gateway.add(items).execute().run { check(isSuccessful) { errorBody()!!.string() } }
        val response = gateway.checkout(CheckoutRequestBody.free()).execute()
        assertTrue(response.errorBody()?.string(), response.isSuccessful)
        assertEquals(STATUS_CHECKOUT_PAID, response.body()!!.status)
    }

    private companion object {
        private const val ERROR_CODE_ELIGIBILITY = "eligibility"
        private const val STATUS_CHECKOUT_PAID = "Paid"
    }
}
