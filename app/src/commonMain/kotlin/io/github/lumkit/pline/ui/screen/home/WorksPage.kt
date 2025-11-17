package io.github.lumkit.pline.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
            items = filterWorks,
        ) { work ->
            WorkItem(
                viewModel = viewModel,
                work = work,
            ) {
                navHostController.navigateToPaint(work.id)
            }
        }
    }

}

@Composable
private fun WorkItem(
    viewModel: HomeViewModel,
    work: Work,
    onClick: (Work) -> Unit,
) {
    val density = LocalDensity.current
    var formattedDate by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var height by rememberSaveable(work.id) { mutableStateOf(0f) }
    val imageData by derivedStateOf(referentialEqualityPolicy()) {
        work.coverThumb
    }

    LaunchedEffect(work) {
        formattedDate = formatTimestamp(work.updateAt)
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
        AsyncImage(
            model = imageData,
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
                    Popup(
                        onDismissRequest = { expanded = false },
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
                                    scope.launch {
                                        viewModel.deleteWork(work)
                                    }
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
            }
        }
    }
}
