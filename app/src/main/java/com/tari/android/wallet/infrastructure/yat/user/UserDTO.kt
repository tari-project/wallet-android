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
package com.tari.android.wallet.infrastructure.yat.user

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class UserDTO(
    @Expose @SerializedName("id") val id: String,
    @Expose @SerializedName("alternate_id") val alternateId: String,
    @Expose @SerializedName("email") val email: String,
    @Expose @SerializedName("first_name") val firstName: String,
    @Expose @SerializedName("last_name") val lastName: String,
    @Expose @SerializedName("role") val role: String,
    @Expose @SerializedName("two_factor_auth") val twoFactorAuth: Any,
    @Expose @SerializedName("free_limit") val freeEmojiIdsQuantity: Int,
    @Expose @SerializedName("remaining_free_emoji") val freeEmojiIdsLeft: Int,
    @Expose @SerializedName("is_active") val isActive: Boolean,
    @Expose @SerializedName("created_at") val createdAt: String,
    @Expose @SerializedName("updated_at") val updatedAt: String,
)
