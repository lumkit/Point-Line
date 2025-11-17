package io.github.lumkit.pline.ui.screen.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.lumkit.pline.config.LocalScreenNavController
import io.github.lumkit.pline.ui.DefaultDuration
import io.github.lumkit.pline.ui.screen.navigateToPaint
import io.github.lumkit.pline.ui.theme.*
import kotlinx.coroutines.async
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import point_line.app.generated.resources.Res
import point_line.app.generated.resources.add_work_paint
import point_line.app.generated.resources.ic_paint_filled
import kotlin.math.roundToInt

@Composable
fun AddWorkPopup(
    addPopupState: MutableState<Boolean>,
) {
    val density = LocalDensity.current
    val navHostController = LocalScreenNavController.current

    var visible by rememberSaveable { mutableStateOf(false) }
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(.5f) }
    val offsetYPx = remember { with(density) { AppBarHeight.toPx() * .8f } }
    val offsetYAnim = remember { Animatable(offsetYPx) }

    LaunchedEffect(addPopupState) {
        snapshotFlow { addPopupState.value }
            .collect {
                if (it) {
                    visible = true
                }
                val alphaAnimJob = async {
                    alphaAnim.animateTo(
                        targetValue = if (it) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = DefaultDuration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                val scaleAnimJob = async {
                    scaleAnim.animateTo(
                        targetValue = if (it) 1f else .5f,
                        animationSpec = tween(
                            durationMillis = DefaultDuration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                val offsetYAnimJob = async {
                    offsetYAnim.animateTo(
                        targetValue = if (it) 0f else offsetYPx,
                        animationSpec = tween(
                            durationMillis = DefaultDuration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                if (!it) {
                    alphaAnimJob.await()
                    scaleAnimJob.await()
                    offsetYAnimJob.await()
                    if (!addPopupState.value) visible = false
                }
            }
    }

    if (visible) {
        Popup(
            alignment = Alignment.BottomCenter,
            offset = with(density) {
                IntOffset(
                    0,
                    -LargeAppBarHeight.toPx().roundToInt() -
                            SmallSpacing.toPx().roundToInt() -
                            with(density) {
                                WindowInsets.navigationBars.getBottom(this)
                            }
                )
            },
            onDismissRequest = { addPopupState.value = false },
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                clippingEnabled = false,
            )
        ) {
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .alpha(alphaAnim.value)
                        .scale(scaleAnim.value)
                        .offset { IntOffset(0, offsetYAnim.value.roundToInt()) },
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colorScheme.surface,
                        contentColor = AppTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = AppTheme.colorScheme.primaryContainer
                    ),
                ) {
                    FlowRow(
                        modifier = Modifier.padding(LargeSpacing),
                        verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                        horizontalArrangement = Arrangement.spacedBy(SmallSpacing, Alignment.CenterHorizontally),
                    ) {
                        Item(
                            painter = painterResource(Res.drawable.ic_paint_filled),
                            label = stringResource(Res.string.add_work_paint),
                            onClick = {
                                // 点击绘画项时，传递 null 表示新建一个作品
                                navHostController.navigateToPaint(null)
                                addPopupState.value = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Item(
    painter: Painter,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(AppBarHeight)
            .clickable(
                indication = null,
                interactionSource = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ExtraSmallSpacing)
    ) {
        Card(
            modifier = Modifier.size(AppBarHeight),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(
                defaultElevation = ExtraSmallSpacing,
                pressedElevation = ExtraSmallSpacing,
                focusedElevation = ExtraSmallSpacing,
                disabledElevation = 0.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = AppTheme.colorScheme.primaryContainer,
                contentColor = AppTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(LargeIconSize)
                )
            }
        }

        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colorScheme.text
        )
    }
}
