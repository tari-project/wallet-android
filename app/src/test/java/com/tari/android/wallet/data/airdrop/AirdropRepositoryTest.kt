package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirdropRepositoryTest {

    private val retrofit = mockk<AirdropRetrofitService>()
    private val corePrefRepository = mockk<CorePrefRepository>(relaxed = true)
    private val repository = AirdropRepository(retrofit, corePrefRepository)

    private fun user(displayName: String?, name: String) = UserDetailsResponse(
        UserDetailsResponse.User(
            imageUrl = null,
            id = "id",
            referralCode = "code",
            yatUserId = null,
            displayName = displayName,
            name = name,
            profileImageUrl = "",
            rank = null,
        )
    )

    @Test
    fun `verify account refreshes the candidate pair and looks up the new token`() = runBlocking {
        coEvery { retrofit.refreshAuthToken(RefreshTokenRequest("candidate-refresh")) } returns RefreshTokenResponse("new-token", "new-refresh")
        coEvery { retrofit.getUserDetails("Bearer new-token") } returns user(displayName = "alice", name = "Alice Name")

        val account = repository.verifyAccount("candidate-refresh")

        assertEquals(VerifiedAirdropAccount("new-token", "new-refresh", "alice"), account)
        verify(exactly = 0) { corePrefRepository.airdropToken = any() }
        verify(exactly = 0) { corePrefRepository.airdropRefreshToken = any() }
    }

    @Test
    fun `verify account falls back to name when display name is blank`() = runBlocking {
        coEvery { retrofit.refreshAuthToken(any()) } returns RefreshTokenResponse("new-token", "new-refresh")
        coEvery { retrofit.getUserDetails(any()) } returns user(displayName = " ", name = "Alice Name")

        assertEquals("Alice Name", repository.verifyAccount("candidate-refresh")?.name)
    }

    @Test
    fun `verify account fails when refresh fails`() = runBlocking {
        coEvery { retrofit.refreshAuthToken(any()) } throws RuntimeException("401")

        assertNull(repository.verifyAccount("candidate-refresh"))
        coVerify(exactly = 0) { retrofit.getUserDetails(any()) }
    }

    @Test
    fun `verify account fails when details fail`() = runBlocking {
        coEvery { retrofit.refreshAuthToken(any()) } returns RefreshTokenResponse("new-token", "new-refresh")
        coEvery { retrofit.getUserDetails(any()) } throws RuntimeException("401")

        assertNull(repository.verifyAccount("candidate-refresh"))
    }

    @Test
    fun `verify account fails when the account has no name`() = runBlocking {
        coEvery { retrofit.refreshAuthToken(any()) } returns RefreshTokenResponse("new-token", "new-refresh")
        coEvery { retrofit.getUserDetails(any()) } returns user(displayName = null, name = "")

        assertNull(repository.verifyAccount("candidate-refresh"))
    }

    @Test
    fun `verify account shows a sanitised name and falls back when display name has nothing left`() = runBlocking {
        coEvery { retrofit.refreshAuthToken(any()) } returns RefreshTokenResponse("new-token", "new-refresh")
        coEvery { retrofit.getUserDetails(any()) } returns user(displayName = "\u202E\u200B", name = "Alice\nSmith")

        assertEquals("Alice Smith", repository.verifyAccount("candidate-refresh")?.name)
    }

    @Test
    fun `verify account fails when no name survives sanitising`() = runBlocking {
        coEvery { retrofit.refreshAuthToken(any()) } returns RefreshTokenResponse("new-token", "new-refresh")
        coEvery { retrofit.getUserDetails(any()) } returns user(displayName = "\u202E", name = "\u200B\n")

        assertNull(repository.verifyAccount("candidate-refresh"))
    }
}
