package com.tari.android.wallet.ui.screen.debug.sampleDesign

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.PreviewPrimarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariInheritTextButton
import com.tari.android.wallet.ui.compose.components.TariOutlinedButton
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariSearchField
import com.tari.android.wallet.ui.compose.components.TariSecondaryButton
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.compose.components.TariTextField
import com.tari.android.wallet.ui.compose.widgets.StartMiningButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme


@Composable
fun SampleDesignSystemScreen(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = TariDesignSystem.colors.backgroundPrimary,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Heading 2XLarge Typography",
                style = TariDesignSystem.typography.heading2XLarge,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Heading XLarge Typography",
                style = TariDesignSystem.typography.headingXLarge,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Heading Large Typography",
                style = TariDesignSystem.typography.headingLarge,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Heading Medium Typography",
                style = TariDesignSystem.typography.headingMedium,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Heading Small Typography",
                style = TariDesignSystem.typography.headingSmall,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Body1 Typography",
                style = TariDesignSystem.typography.body1,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Body2 Typography",
                style = TariDesignSystem.typography.body2,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Modal Title Large Typography",
                style = TariDesignSystem.typography.modalTitleLarge,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Modal Title Typography",
                style = TariDesignSystem.typography.modalTitle,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Menu Item Typography",
                style = TariDesignSystem.typography.menuItem,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Button Text Typography",
                style = TariDesignSystem.typography.buttonText,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Button Large Typography",
                style = TariDesignSystem.typography.buttonLarge,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Button Medium Typography",
                style = TariDesignSystem.typography.buttonMedium,
            )

            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Button Small Typography",
                style = TariDesignSystem.typography.buttonSmall,
            )

            TariPrimaryButton(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Primary Button Large",
                onClick = { },
            )

            TariPrimaryButton(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Primary Button Medium",
                size = TariButtonSize.Medium,
                onClick = { },
            )

            TariPrimaryButton(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Primary Button Small",
                size = TariButtonSize.Small,
                onClick = { },
            )

            TariPrimaryButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Primary Button Disabled",
                onClick = { },
                enabled = false,
            )

            TariSecondaryButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Secondary Button",
                onClick = { },
            )

            TariSecondaryButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Secondary Button Disabled",
                onClick = { },
                enabled = false,
            )

            TariOutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Outlined Button",
                onClick = { },
            )

            TariOutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Outlined Button Disabled",
                onClick = { },
                enabled = false,
            )

            TariTextButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = "Text Button",
                onClick = { },
            )

            TariTextButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = "Text Button Disabled",
                onClick = { },
                enabled = false,
            )

            TariInheritTextButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = "Inherit Text Button",
                onClick = { },
            )

            TariInheritTextButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = "Inherit Text Button Disabled",
                onClick = { },
                enabled = false,
            )

            var isMining by rememberSaveable { mutableStateOf(false) }
            StartMiningButton(
                isMining = isMining,
                onStartMiningClick = { isMining = !isMining },
                modifier = Modifier.padding(20.dp),
            )

            var textFieldValue by rememberSaveable { mutableStateOf("") }
            TariTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                value = textFieldValue,
                onValueChanged = { textFieldValue = it },
                hint = "Hint",
            )

            var searchFieldValue by rememberSaveable { mutableStateOf("") }
            TariSearchField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                searchQuery = searchFieldValue,
                onQueryChanged = { searchFieldValue = it },
                hint = "Search hint",
            )

            Spacer(Modifier.size(80.dp))
        }
    }
}

@Preview
@Composable
private fun LightThemePreview() {
    PreviewPrimarySurface(TariTheme.Light) {
        SampleDesignSystemScreen()
    }
}

@Preview
@Composable
private fun DarkThemePreview() {
    PreviewPrimarySurface(TariTheme.Dark) {
        SampleDesignSystemScreen()
    }
}