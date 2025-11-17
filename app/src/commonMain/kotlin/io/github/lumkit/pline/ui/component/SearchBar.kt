package io.github.lumkit.pline.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lumkit.pline.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import point_line.app.generated.resources.Res
import point_line.app.generated.resources.ic_search

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    hint: String = "",
    onSearch: (String) -> Unit = {},
) {
    var text by rememberSaveable { mutableStateOf("") }

    Row(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colorScheme.surface)
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = LargeSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MediumSpacing),
    ) {
        BasicTextField(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
            value = text,
            onValueChange = {
                text = it
                if (text.isEmpty()) {
                    onSearch("")
                }
            },
            textStyle = AppTheme.typography.bodyMedium,
            singleLine = true,
            cursorBrush = TextFieldDefaults.cursorBrush,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onSearch(text) },
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.padding(start = ExtraSmallSpacing)
                ) {
                    AnimatedVisibility(
                        visible = text.isEmpty()
                    ) {
                        Text(
                            text = hint,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colorScheme.text.copy(alpha = 0.4f),
                        )
                    }
                }
                innerTextField()
            }
        )

        Icon(
            painter = painterResource(Res.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(MediumIconSize),
            tint = AppTheme.colorScheme.text.copy(alpha = 0.5f)
        )
    }
}
