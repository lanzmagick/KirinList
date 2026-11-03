package com.axiel7.anihyou.ui.screens.main.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import com.axiel7.anihyou.core.ui.common.BottomDestination
import com.axiel7.anihyou.core.ui.common.BottomDestination.Companion.testTag
import com.axiel7.anihyou.core.ui.common.LocalNavActionManager
import com.axiel7.anihyou.core.ui.common.navigation.Route
import kotlinx.coroutines.launch

@Composable
fun MainBottomNavBar(
    currentTopRoute: Route,
    isVisible: Boolean,
    onItemSelected: (Int) -> Unit,
) {
    val navActionManager = LocalNavActionManager.current
    val animationScope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        // Original NavigationBar implementation is kept below as comments.
        /*
        NavigationBar {
            BottomDestination.values.forEachIndexed { index, dest ->
                val isSelected = dest.route == currentTopRoute

                val image = AnimatedImageVector.animatedVectorResource(dest.icon)
                var atEnd by rememberSaveable { mutableStateOf(isSelected) }

                LaunchedEffect(isSelected) {
                    atEnd = isSelected
                }

                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = rememberAnimatedVectorPainter(image, atEnd),
                            contentDescription = stringResource(dest.title),
                        )
                    },
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                        testTag = dest.testTag
                    },
                    label = {
                        Text(
                            text = stringResource(dest.title),
                            textAlign = TextAlign.Center
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            when (dest) {
                                BottomDestination.Explore -> {
                                    navActionManager.toSearch()
                                }

                                else -> {}
                            }
                        } else {
                            atEnd = !atEnd
                            onItemSelected(index)
                            navActionManager.navigate(dest.route)
                        }
                    }
                )
            }
        }
        */

        // Pogo-style bottom bar: animated pill background and a colored active icon.
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            val itemColors = listOf(
                Color(0xFFFF8A65), // Home - orange
                Color(0xFF64B5F6), // Anime - blue
                Color(0xFFF06292), // Manga - pink
                Color(0xFFFFB74D), // Profile - amber
                Color(0xFF9575CD)  // Explore - purple
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomDestination.values.forEachIndexed { index, dest ->
                    val isSelected = dest.route == currentTopRoute
                    val activeColor = itemColors[index % itemColors.size]
                    val image = AnimatedImageVector.animatedVectorResource(dest.icon)
                    var atEnd by rememberSaveable { mutableStateOf(isSelected) }
                    val iconFlight = remember(dest.route) { Animatable(0f) }

                    LaunchedEffect(isSelected) {
                        atEnd = isSelected
                    }

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.16f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.48f,
                            stiffness = 520f
                        ),
                        label = "pogoIconScale"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) activeColor
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 220),
                        label = "pogoIconTint"
                    )

                    val itemBackground by animateColorAsState(
                        targetValue = if (isSelected) activeColor.copy(alpha = 0.18f)
                        else Color.Transparent,
                        animationSpec = tween(durationMillis = 220),
                        label = "pogoItemBackground"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(itemBackground)
                            .semantics {
                                testTagsAsResourceId = true
                                testTag = dest.testTag
                            }
                            .clickable {
                                // Pogo flight animation: lift the tapped icon, then let it land.
                                animationScope.launch {
                                    iconFlight.snapTo(0f)
                                    iconFlight.animateTo(
                                        targetValue = -13f,
                                        animationSpec = tween(durationMillis = 110)
                                    )
                                    iconFlight.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.38f,
                                            stiffness = 520f
                                        )
                                    )
                                }

                                if (isSelected) {
                                    when (dest) {
                                        BottomDestination.Explore -> navActionManager.toSearch()
                                        else -> {}
                                    }
                                } else {
                                    atEnd = !atEnd
                                    onItemSelected(index)
                                    navActionManager.navigate(dest.route)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = rememberAnimatedVectorPainter(image, atEnd),
                            contentDescription = stringResource(dest.title),
                            tint = iconTint,
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        )
                    }
                }
            }
        }
    }
}
