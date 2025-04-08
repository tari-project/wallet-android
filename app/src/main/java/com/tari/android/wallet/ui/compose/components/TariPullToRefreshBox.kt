package com.tari.android.wallet.ui.compose.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariPullToRefreshBox(
    onPullToRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var isLoading by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(1000)
            isLoading = false
        }
    }

    PullToRefreshBox(
        modifier = modifier,
        state = pullToRefreshState,
        isRefreshing = isLoading,
        onRefresh = {
            isLoading = true
            onPullToRefresh()
        },
        content = content,
    )
}