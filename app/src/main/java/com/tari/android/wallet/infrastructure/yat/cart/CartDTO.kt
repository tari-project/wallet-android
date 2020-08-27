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
package com.tari.android.wallet.infrastructure.yat.cart

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.tari.android.wallet.infrastructure.yat.user.UserDTO

//{
//    "id": "8892b00c-8e4e-4f2a-9f2f-f1b1960979bf",
//    "user_id": "447c0483-64f3-407f-af0f-65e14a8ff359",
//    "organization_id": null,
//    "status": "Draft",
//    "paid_at": null,
//    "checkout_url": null,
//    "checkout_url_expires_at": null,
//    "expires_at": "2020-09-01T11:21:57.766531Z",
//    "created_at": "2020-09-01T11:06:57.755070Z",
//    "updated_at": "2020-09-01T11:06:57.772383Z",
//    "order_number": "960979bf",
//    "order_items": [
//        {
//            "id": "af170462-807c-43bd-86ab-b2eec9ebe31e",
//            "order_id": "8892b00c-8e4e-4f2a-9f2f-f1b1960979bf",
//            "parent_id": null,
//            "emoji_id": "🀄🆒🃏",
//            "item_type": "EmojiId",
//            "quantity": 1,
//            "refunded_quantity": 0,
//            "unit_price_in_cents": 1500,
//            "company_fee_in_cents": 0,
//            "client_fee_in_cents": 0,
//            "code_id": null,
//            "created_at": "2020-09-01T11:06:57.762788Z",
//            "updated_at": "2020-09-01T11:06:57.762788Z"
//        }
//    ],
//    "user": {
//        "id": "447c0483-64f3-407f-af0f-65e14a8ff359",
//        "email": "email@email.email",
//        "first_name": null,
//        "last_name": null,
//        "role": "User",
//        "two_factor_auth": null,
//        "free_limit": 1,
//        "remaining_free_emoji": 1,
//        "is_active": true,
//        "created_at": "2020-08-31T13:38:13.674765Z",
//        "updated_at": "2020-09-01T11:06:57.756851Z"
//    },
//    "total_in_cents": 1500,
//    "seconds_until_expiry": 899,
//    "payment_method_data": null,
//    "eligible_for_refund": false,
//    "misc_refunded_total_in_cents": 0,
//    "refunded_total_in_cents": 0
//}

data class CartDTO(
    @Expose @SerializedName("id") val id: String,
    @Expose @SerializedName("user_id") val userId: String,
    // TODO(nyarian): refactor to use enum once it's documented
    @Expose @SerializedName("status") val status: String,
    @Expose @SerializedName("order_number") val orderNumber: String,
    @Expose @SerializedName("total_in_cents") val totalInCents: Int,
    @Expose @SerializedName("seconds_until_expiry") val secondsUntilExpiry: Int,
    @Expose @SerializedName("order_items") val orderItems: List<OrderItemDTO>,
    @Expose @SerializedName("user") val user: UserDTO,
)

data class OrderItemDTO(
    @Expose @SerializedName("id") val id: String,
    @Expose @SerializedName("order_id") val orderId: String,
    @Expose @SerializedName("emoji_id") val emojiId: String?,
    @Expose @SerializedName("item_type") val itemType: String,
    @Expose @SerializedName("quantity") val quantity: Int,
    @Expose @SerializedName("unit_price_in_cents") val unitPriceInCents: Int,
)
