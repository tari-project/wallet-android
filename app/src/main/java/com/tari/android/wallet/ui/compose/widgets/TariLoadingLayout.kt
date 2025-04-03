package com.tari.android.wallet.ui.compose.widgets

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun TariLoadingLayout(
    modifier: Modifier = Modifier,
    targetLoadingState: TariLoadingLayoutState,
    errorLayout: @Composable () -> Unit = { TariErrorView {} },
    loadingLayout: @Composable () -> Unit = { TariProgressView() },
    contentLayout: @Composable ColumnScope.() -> Unit = {},
) {
    Crossfade(
        modifier = modifier.animateContentSize(),
        targetState = targetLoadingState,
    ) { loadingState ->
        when (loadingState) {
            is TariLoadingLayoutState.Error -> errorLayout()
            is TariLoadingLayoutState.Loading -> loadingLayout()
            is TariLoadingLayoutState.Content -> Column { contentLayout() }
        }
    }
}

@Composable
fun TariErrorView(
    modifier: Modifier = Modifier,
    errorTitle: String = stringResource(R.string.common_error_title),
    errorMessage: String = stringResource(R.string.common_error_description),
    onTryAgainClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = errorTitle,
            style = TariDesignSystem.typography.headingLarge,
        )
        Text(
            text = errorMessage,
            style = TariDesignSystem.typography.body2.copy(color = TariDesignSystem.colors.textPrimary),
        )
        Spacer(modifier = Modifier.size(12.dp))
        TariPrimaryButton(
            size = TariButtonSize.Small,
            text = stringResource(R.string.common_error_try_again_button),
            onClick = onTryAgainClick,
        )
    }
}

@Composable
fun TariProgressView(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = TariDesignSystem.colors.primaryMain,
        )
    }
}

sealed interface TariLoadingLayoutState {
    object Error : TariLoadingLayoutState
    object Loading : TariLoadingLayoutState
    object Content : TariLoadingLayoutState
}

@Composable
@Preview
private fun TariLoadingLayoutPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        TariLoadingLayout(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            targetLoadingState = TariLoadingLayoutState.Content,
        ) {
            Text("Content")
        }
    }
}

@Composable
@Preview
fun TariErrorViewPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        TariErrorView(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            onTryAgainClick = {},
        )
    }
}

@Composable
@Preview
fun TariProgressViewPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        TariProgressView(Modifier
            .fillMaxWidth()
            .height(200.dp))
    }
}
