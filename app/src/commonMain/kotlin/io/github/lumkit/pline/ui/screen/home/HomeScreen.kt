package io.github.lumkit.pline.ui.screen.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.lumkit.pline.db.AppDatabase
import io.github.lumkit.pline.ui.DefaultDuration
import io.github.lumkit.pline.ui.screen.ScreenRoute
import io.github.lumkit.pline.ui.theme.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import point_line.app.generated.resources.*

fun NavGraphBuilder.homeScreen() {
    composable<ScreenRoute.Home> {
        HomeScreen()
    }
}

enum class HomePage {
    Works,
    Me
}

internal fun HomePage.asLabelRes(): StringResource {
    return when (this) {
        HomePage.Works -> Res.string.tab_product
        HomePage.Me -> Res.string.tab_me
    }
}

@Composable
fun HomeScreen() {
    val pagerState = rememberPagerState { HomePage.entries.size }
    val addPopupState = rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppTheme.colorScheme.secondarySurface,
    ) {
        Box {
            HomePager(pagerState = pagerState)
            HomeBottomBar(
                pagerState = pagerState,
                addPopupState = addPopupState,
            ) {
                addPopupState.value = !addPopupState.value
            }
        }
    }

    AddWorkPopup(addPopupState)
}

@Composable
private fun HomePager(pagerState: PagerState) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = HomePage.entries.size,
    ) {
        when (HomePage.entries[it]) {
            HomePage.Works -> WorksPage()
            HomePage.Me -> MePage()
        }
    }
}

@Preview
@Composable
private fun HomeBottomBarPreview() {
    Box(
        modifier = Modifier.height(100.dp)
    ) {
        HomeBottomBar(
            pagerState = PagerState { 2 },
            addPopupState = rememberSaveable { mutableStateOf(false) },
        ) {

        }
    }
}

@Composable
private fun BoxScope.HomeBottomBar(
    pagerState: PagerState,
    addPopupState: State<Boolean>,
    onAddClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val bottomWindowInsets = BottomAppBarDefaults.windowInsets

    val iconAlpha by animateFloatAsState(
        if (!addPopupState.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = DefaultDuration,
            easing = FastOutSlowInEasing,
        )
    )
    val iconRotate by animateFloatAsState(
        if (!addPopupState.value) 0f else 180f,
        animationSpec = tween(
            durationMillis = DefaultDuration,
            easing = FastOutSlowInEasing,
        )
    )

    Box(
        modifier = Modifier.fillMaxWidth()
            .align(Alignment.BottomCenter),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .shadow(SmallSpacing, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AppTheme.colorScheme.surface)
                .windowInsetsPadding(bottomWindowInsets)
                .height(AppBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomBarItem(
                selected = pagerState.currentPage == HomePage.Works.ordinal,
                modifier = Modifier.weight(1f),
                icon = painterResource(Res.drawable.ic_products),
                enabled = !addPopupState.value,
            ) {
                scope.launch {
                    pagerState.animateScrollToPage(HomePage.Works.ordinal)
                }
            }
            Spacer(modifier = Modifier.width(LargeAppBarHeight))
            BottomBarItem(
                selected = pagerState.currentPage == HomePage.Me.ordinal,
                modifier = Modifier.weight(1f),
                icon = painterResource(Res.drawable.ic_me),
                enabled = !addPopupState.value,
            ) {
                scope.launch {
                    pagerState.animateScrollToPage(HomePage.Me.ordinal)
                }
            }
        }

        Column {
            Card(
                modifier = Modifier.clip(CircleShape)
                    .background(AppTheme.colorScheme.surface)
                    .size(LargeAppBarHeight)
                    .padding(SmallSpacing),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colorScheme.surface,
                ),
                shape = CircleShape,
                onClick = onAddClick,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(AppTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_start_paint),
                        contentDescription = "",
                        modifier = Modifier.size(LargeIconSize)
                            .rotate(iconRotate)
                            .alpha(iconAlpha),
                        tint = AppTheme.colorScheme.primary
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_cross),
                        contentDescription = "",
                        modifier = Modifier.size(LargeIconSize)
                            .rotate(iconRotate)
                            .alpha(1f - iconAlpha),
                        tint = AppTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.windowInsetsPadding(bottomWindowInsets))
        }
    }
}

@Composable
private fun BottomBarItem(
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: Painter,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colorAnimation by animateColorAsState(
        targetValue = if (!enabled) {
            AppTheme.colorScheme.text.copy(alpha = 0.21f)
        } else if (selected) {
            AppTheme.colorScheme.primary
        } else {
            AppTheme.colorScheme.primaryContainer
        }
    )

    Box(
        modifier = modifier.fillMaxHeight()
            .clickable(
                indication = null,
                interactionSource = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = "",
            modifier = Modifier.size(LargeIconSize),
            tint = colorAnimation,
        )
    }
}
