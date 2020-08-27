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

//{
//    "alternate_id": "D4E86EF8EC383FB8011DB68F95B1007763D57489FB46A18907E414E9FAA5A81E",
//    "password": "password",
//    "first_name": "fn",
//    "last_name": "ln",
//    "source": "Aurora"
//}

data class UserRegistrationRequestBody(
    @Expose @SerializedName("alternate_id") val alternate_id: String,
    @Expose @SerializedName("password") val password: String,
    @Expose @SerializedName("first_name") val firstName: String? = null,
    @Expose @SerializedName("last_name") val lastName: String? = null,
    @Expose @SerializedName("source") val source: String? = "Aurora",
)

/*
{
	"user": {
		"id": "1c800be7-9d0d-43cc-906d-a18ed931ff7b",
		"email": null,
		"alternate_id": "D4E86EF8EC383FB8011DB68F95B1007763D57489FB46A18907E414E9FAA5A81E",
		"first_name": null,
		"last_name": null,
		"role": "User",
		"two_factor_auth": null,
		"free_limit": 1,
		"remaining_free_emoji": 1,
		"is_active": false,
		"source": "Aurora",
		"pubkeys": ["1aa6e96e38029efdc21da9affcde55cb6feb249cc5ad8c3bfd245a5a72321842"],
		"emoji_ids": [],
		"created_at": "2021-03-02T08:36:47.410614Z",
		"updated_at": "2021-03-02T08:36:47.410614Z"
	},
	"role": "User",
	"global_scopes": [],
	"organization_roles": {},
	"organization_scopes": {},
	"pubkeys": ["1aa6e96e38029efdc21da9affcde55cb6feb249cc5ad8c3bfd245a5a72321842"],
	"pending_transfers": []
}
 */

data class UserRegistrationResponseBody(
    @Expose @SerializedName("role") val role: String,
    @Expose @SerializedName("user") val user: UserDTO,
    @Expose @SerializedName("global_scopes") val globalScopes: List<String>,
    @Expose @SerializedName("organization_roles") val organizationRoles: OrganizationRoles,
    @Expose @SerializedName("organization_scopes") val organizationScopes: OrganizationScopes,
    @Expose @SerializedName("pubkeys") val pubkeys: List<String>,
)

class OrganizationRoles

class OrganizationScopes
