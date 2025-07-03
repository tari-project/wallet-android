package com.tari.android.wallet.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorText: String? = null,
) {
    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier
                .fillMaxWidth()
                .clip(TariDesignSystem.shapes.card)
                .border(1.dp, TariDesignSystem.colors.elevationOutlined, shape = TariDesignSystem.shapes.card)
                .indicatorLine(
                    enabled = false,
                    isError = false,
                    interactionSource = remember { MutableInteractionSource() },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    focusedIndicatorLineThickness = 0.dp,
                    unfocusedIndicatorLineThickness = 0.dp
                ),
            placeholder = {
                Text(
                    text = hint,
                    style = TariDesignSystem.typography.body1,
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TariDesignSystem.colors.backgroundPrimary,
                focusedTextColor = TariDesignSystem.colors.textPrimary,
                unfocusedContainerColor = TariDesignSystem.colors.backgroundPrimary,
                unfocusedTextColor = TariDesignSystem.colors.textPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = TariDesignSystem.colors.primaryMain,
            ),
            textStyle = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
        )

        if (errorText != null) {
            Spacer(Modifier.size(8.dp))
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = errorText,
                style = TariDesignSystem.typography.body2.copy(color = TariDesignSystem.colors.errorMain),
            )
        }
    }
}

@Composable
fun TariSearchField(
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    TariTextField(
        value = searchQuery,
        onValueChanged = onQueryChanged,
        hint = hint,
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                tint = TariDesignSystem.colors.componentsNavbarIcons,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        tint = TariDesignSystem.colors.componentsNavbarIcons,
                        contentDescription = null,
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun TariTextFieldPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            value = "",
            onValueChanged = {},
            hint = "Hint",
        )

        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            value = "Value",
            onValueChanged = {},
            hint = "Hint",
        )


        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            value = "Value",
            onValueChanged = {},
            hint = "Hint",
            errorText = "Error text here",
        )

        TariSearchField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            searchQuery = "Query",
            onQueryChanged = {},
            hint = "Hint",
        )

        TariSearchField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            searchQuery = "",
            onQueryChanged = {},
            hint = "Hint",
        )
    }
}