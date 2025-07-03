package com.tari.android.wallet.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariTextField(
    value: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleAdditionalLayout: @Composable (() -> Unit) = {},
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorText: String? = null,
    numberKeyboard: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            title?.let { title ->
                Column {
                    Text(
                        text = title,
                        style = TariDesignSystem.typography.body1.copy(
                            color = if (errorText.isNullOrBlank()) TariDesignSystem.colors.textPrimary else TariDesignSystem.colors.errorMain
                        ),
                    )
                    Spacer(Modifier.size(8.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            titleAdditionalLayout()
        }

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
            keyboardOptions = if (numberKeyboard) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            visualTransformation = if (numberKeyboard) AmountVisualTransformation() else VisualTransformation.None,
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

class AmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Split the text into integer and fractional parts
        val parts = text.text.split('.', limit = 2)
        val intPart = parts.getOrNull(0) ?: ""
        val fracPart = parts.getOrNull(1)
        // Format the integer part with thousand separators; fallback to plain text if not a valid number
        val formattedInt = intPart.toLongOrNull()?.let {
            NumberFormat.getInstance().format(it)
        } ?: intPart

        val newText = if (fracPart != null) "$formattedInt.$fracPart" else formattedInt

        // Since the formatting changes the length or position of characters, use a simple OffsetMapping
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = newText.length
            override fun transformedToOriginal(offset: Int): Int = text.text.length
        }

        return TransformedText(AnnotatedString(newText), offsetMapping)
    }
}

@Composable
fun TariSearchField(
    searchQuery: TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
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
            if (searchQuery.text.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged(TextFieldValue("")) }) {
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
            value = TextFieldValue(""),
            onValueChanged = {},
            hint = "Hint",
        )

        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            value = TextFieldValue("Value"),
            onValueChanged = {},
            hint = "Hint",
            title = "Address",
        )

        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            value = TextFieldValue("Value"),
            onValueChanged = {},
            hint = "Hint",
            title = "Address",
            errorText = "Error text here",
        )

        TariSearchField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            searchQuery = TextFieldValue("Query"),
            onQueryChanged = {},
            hint = "Hint",
        )

        TariSearchField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            searchQuery = TextFieldValue(""),
            onQueryChanged = {},
            hint = "Hint",
        )

        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            value = TextFieldValue("Value"),
            onValueChanged = {},
            hint = "Hint",
            title = "Address",
            titleAdditionalLayout = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        tint = TariDesignSystem.colors.componentsNavbarIcons,
                        contentDescription = null,
                    )
                }
            }
        )

        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            value = TextFieldValue("Value"),
            onValueChanged = {},
            hint = "Hint",
            title = "Address",
            titleAdditionalLayout = {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = "Additional Info",
                    style = TariDesignSystem.typography.body1,
                )
            }
        )
    }
}