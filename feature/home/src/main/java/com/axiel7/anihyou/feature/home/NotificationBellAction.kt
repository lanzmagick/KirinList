package com.axiel7.anihyou.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.axiel7.anihyou.core.resources.R
import com.axiel7.anihyou.core.ui.composables.IconButtonWithBadge

@Composable
internal fun NotificationBellAction(
    unreadCount: Int,
    hasNewNotification: Boolean,
    onClick: () -> Unit,
) {
    // Bell shake animation when there is a new notification
    val bellModifier = if (hasNewNotification) {
        val transition = rememberInfiniteTransition(
            label = "newNotificationBell"
        )

        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2400
                    0f at 0
                    -14f at 150
                    12f at 300
                    -9f at 450
                    6f at 600
                    0f at 780
                    0f at 2400
                }
            ),
            label = "bellShakeAngle",
        )

        Modifier.graphicsLayer {
            rotationZ = angle
            transformOrigin = TransformOrigin(0.5f, 0.1f)
        }
    } else Modifier

    IconButtonWithBadge(
        icon = R.drawable.notifications_24,
        iconModifier = bellModifier,
        badge = {
            if (hasNewNotification) {
                PulsingNotificationDot()
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun PulsingNotificationDot() {
    val pulse = rememberInfiniteTransition(
        label = "notificationDotPulse"
    )

    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = LinearEasing
            )
        ),
        label = "pulsePhase",
    )

    val dotColor = MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(9.dp)
                .graphicsLayer {
                    scaleX = 1f + phase * 0.9f
                    scaleY = 1f + phase * 0.9f
                    alpha = 1f - phase
                }
                .border(
                    width = 1.5.dp,
                    color = dotColor,
                    shape = CircleShape
                )
        )

        // Inner red dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color = dotColor,
                    shape = CircleShape
                )
        )
    }
}
