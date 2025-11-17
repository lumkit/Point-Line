package io.github.lumkit.pline.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import io.github.lumkit.pline.config.LocalScreenNavController
import io.github.lumkit.pline.db.AppDatabase
import io.github.lumkit.pline.db.entity.Work
import io.github.lumkit.pline.ui.component.AppScaffold
import io.github.lumkit.pline.ui.component.SearchBar
import io.github.lumkit.pline.ui.popup.SmartPopupPositionProvider
import io.github.lumkit.pline.ui.screen.navigateToPaint
import io.github.lumkit.pline.ui.theme.*
import io.github.lumkit.pline.ui.theme.shape.LargeShape
import io.github.lumkit.pline.util.formatTimestamp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import point_line.app.generated.resources.*

@Composable
fun WorksPage(
    viewModel: HomeViewModel = viewModel {
        HomeViewModel(AppDatabase.instance)
    }
) {
    val searchTextState = rememberSaveable { mutableStateOf("") }
    val aliveWorks by viewModel.aliveWorks.collectAsState()
    var filterWorks by remember { mutableStateOf(emptyList<Work>()) }
    val lazyStaggeredGridState = rememberLazyStaggeredGridState()

    LaunchedEffect(aliveWorks) {
        snapshotFlow { searchTextState.value }
            .collect {
                filterWorks = aliveWorks.filter { work ->
                    work.label.contains(it, ignoreCase = true)
                }
            }
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppTheme.colorScheme.secondarySurface,
    ) {
        Column(
            modifier = Modifier.padding(it)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .height(AppBarHeight)
                    .padding(horizontal = LargeSpacing),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.text_product_set),
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colorScheme.text,
                )
            }

            Spacer(modifier = Modifier.height(ExtraSmallSpacing))

            SearchBar(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = LargeSpacing),
                hint = stringResource(Res.string.text_search_product)
            ) { text ->
                searchTextState.value = text
            }

            Spacer(modifier = Modifier.height(LargeSpacing))

            AnimatedContent(
                targetState = filterWorks.isEmpty(),
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                }
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyWorksPlaceholder(searchTextState.value.isNotBlank() && aliveWorks.isNotEmpty())
                } else {
                    WorksContent(
                        viewModel = viewModel,
                        filterWorks = filterWorks,
                        lazyStaggeredGridState = lazyStaggeredGridState,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWorksPlaceholder(
    isFiltered: Boolean
) {
    val bottomWindowInsets = BottomAppBarDefaults.windowInsets
    val composition by rememberLottieComposition(
        isFiltered
    ) {
        LottieCompositionSpec.JsonString(
            Res.readBytes(
                if (isFiltered) {
                    "files/anim_not_find.json"
                } else {
                    "files/anim_draw.json"
                }
            ).decodeToString()
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(bottomWindowInsets),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
            ),
            contentDescription = null,
            modifier = Modifier.size(150.dp),
        )

        Spacer(modifier = Modifier.height(MediumSpacing))

        Text(
            text = stringResource(
                if (isFiltered) {
                    Res.string.works_empty_filtered
                } else {
                    Res.string.works_empty
                }
            ),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colorScheme.text.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = LargeAppBarHeight + MediumSpacing)
        )
    }
}

@Composable
private fun WorksContent(
    viewModel: HomeViewModel,
    filterWorks: List<Work>,
    lazyStaggeredGridState: LazyStaggeredGridState,
) {
    val navHostController = LocalScreenNavController.current
    val scope = rememberCoroutineScope()
    var uiItems by remember { mutableStateOf(filterWorks) }
    var exitingFilterIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var exitingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var enteringIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var updatedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(filterWorks) {
        val oldMap = uiItems.associateBy { it.id }
        val newMap = filterWorks.associateBy { it.id }
        val oldIds = oldMap.keys
        val newIds = newMap.keys

        val toAdd = newIds - oldIds
        val toRemove = oldIds - newIds
        val toUpdate = newIds.intersect(oldIds).filter { oldMap[it] != newMap[it] }.toSet()

        enteringIds = (enteringIds + toAdd)
        updatedIds = toUpdate

        // 过滤导致的临时退出：加入 toRemove；如果新过滤又包含，则移除退出标记
        exitingFilterIds = (exitingFilterIds + toRemove) - newIds

        val exitingItems = uiItems.filter { it.id in exitingFilterIds }
        val merged = buildList {
            addAll(filterWorks)
            addAll(exitingItems.filter { it.id !in newIds })
        }
        uiItems = merged
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LargeSpacing,
            end = LargeSpacing,
            bottom = LargeAppBarHeight + SmallSpacing,
        ),
        state = lazyStaggeredGridState,
        horizontalArrangement = Arrangement.spacedBy(SmallSpacing),
        verticalItemSpacing = SmallSpacing,
    ) {
        items(
            items = uiItems,
            key = { it.id }
        ) { work ->
            val isExiting = exitingDeleteIds.contains(work.id) || exitingFilterIds.contains(work.id)
            val isEntering = enteringIds.contains(work.id)
            val isUpdated = updatedIds.contains(work.id)

            AnimatedWorkItem(
                work = work,
                isEntering = isEntering,
                isExiting = isExiting,
                isUpdated = isUpdated,
                onEnterFinished = { id -> enteringIds = enteringIds - id },
                onExitFinished = { w ->
                    if (exitingDeleteIds.contains(w.id)) {
                        exitingDeleteIds = exitingDeleteIds - w.id
                        uiItems = uiItems.filterNot { it.id == w.id }
                        scope.launch { viewModel.deleteWork(w) }
                    } else if (exitingFilterIds.contains(w.id)) {
                        exitingFilterIds = exitingFilterIds - w.id
                        uiItems = uiItems.filterNot { it.id == w.id }
                    }
                }
            ) {
                WorkItem(
                    viewModel = viewModel,
                    work = work,
                    onClick = { navHostController.navigateToPaint(work.id) },
                    onActionTap = { work, event ->
                        when (event) {
                            MoreItemEvent.Delete -> {
                                if (!exitingDeleteIds.contains(work.id)) {
                                    exitingDeleteIds = exitingDeleteIds + work.id
                                }
                            }
                        }
                    }
                )
            }
        }
    }

}

@Composable
private fun AnimatedWorkItem(
    work: Work,
    isEntering: Boolean,
    isExiting: Boolean,
    isUpdated: Boolean,
    onEnterFinished: (Long) -> Unit,
    onExitFinished: (Work) -> Unit,
    content: @Composable () -> Unit,
) {
    val visibleState = remember(work.id, isEntering) { MutableTransitionState(!isEntering) }

    LaunchedEffect(isEntering) {
        if (isEntering) visibleState.targetState = true
    }
    LaunchedEffect(isExiting) {
        if (isExiting) visibleState.targetState = false else visibleState.targetState = true
    }

    LaunchedEffect(visibleState.isIdle, visibleState.currentState, visibleState.targetState) {
        if (visibleState.isIdle && visibleState.currentState) {
            onEnterFinished(work.id)
        }
        if (visibleState.isIdle && !visibleState.currentState && !visibleState.targetState) {
            onExitFinished(work)
        }
    }

    val alphaAnim = remember(work.id) { Animatable(1f) }
    LaunchedEffect(isUpdated, work) {
        if (isUpdated) {
            alphaAnim.snapTo(0.85f)
            alphaAnim.animateTo(1f, tween(220))
        }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(220)) + expandVertically(),
        exit = fadeOut(tween(180)) + shrinkVertically(),
        modifier = Modifier
            .graphicsLayer { alpha = alphaAnim.value }
    ) {
        content()
    }
}

@Composable
private fun WorkItem(
    viewModel: HomeViewModel,
    work: Work,
    onClick: (Work) -> Unit,
    onActionTap: (Work, MoreItemEvent) -> Unit,
) {
    val density = LocalDensity.current
    var formattedDate by remember { mutableStateOf("") }
    var height by rememberSaveable(work.id) { mutableStateOf(0f) }
    var imageData by remember(work.id) { mutableStateOf(work.coverThumb) }

    LaunchedEffect(work) {
        formattedDate = formatTimestamp(work.updateAt)
        imageData = work.coverThumb
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LargeShape,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colorScheme.secondaryContainer,
            contentColor = AppTheme.colorScheme.text,
        ),
        onClick = { onClick(work) },
    ) {
        AnimatedContent(
            targetState = imageData,
            transitionSpec = {
                fadeIn().togetherWith(fadeOut())
            }
        ) {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            bottomStart = LargeSpacing,
                            bottomEnd = LargeSpacing
                        )
                    )
                    .background(color = AppTheme.colorScheme.surface)
                    .onSizeChanged {
                        if (height == 0f) {
                            val fw = (work.frameWidth ?: 1).coerceAtLeast(1)
                            val fh = (work.frameHeight ?: 1)
                            val aspect = fh.toFloat() / fw.toFloat()
                            height = aspect * it.width.toFloat()
                        }
                    }
                    .height(with(density) { height.toDp() }),
                error = painterResource(Res.drawable.ic_file_empty),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = LargeSpacing, vertical = SmallSpacing),
            horizontalArrangement = Arrangement.spacedBy(SmallSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = work.label.takeIf { it.isNotEmpty() }
                        ?: "${stringResource(Res.string.not_naming)}-${work.id}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.text,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = formattedDate,
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colorScheme.text.copy(alpha = 0.6f),
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column {
                var expanded by remember(work.id) { mutableStateOf(false) }
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.size(LargeIconSize)
                        .clip(CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = null,
                        ) {
                            expanded = true
                        }
                )

                if (expanded) {
                    ItemMorePopup(
                        work = work,
                        onDismiss = { expanded = false },
                        onClick = onActionTap,
                    )
                }
            }
        }
    }
}

private enum class MoreItemEvent {
    Delete,
}
@Composable
private fun ItemMorePopup(
    work: Work,
    onDismiss: () -> Unit,
    onClick: (Work, MoreItemEvent) -> Unit,
) {
    val density = LocalDensity.current

    Popup(
        onDismissRequest = onDismiss,
        popupPositionProvider = SmartPopupPositionProvider(
            density = density,
            margin = ExtraSmallSpacing,
        ),
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Card(
            modifier = Modifier.padding(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppTheme.colorScheme.surface,
                contentColor = AppTheme.colorScheme.onSurface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
            )
        ) {
            Row(
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = null,
                ) {
                    onClick(work, MoreItemEvent.Delete)
                    onDismiss()
                }.padding(
                    horizontal = LargeSpacing,
                    vertical = SmallSpacing
                ),
            ) {
                Text(
                    text = stringResource(Res.string.delete),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.text,
                )
            }
        }
    }
}
