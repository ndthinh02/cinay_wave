package com.cinaywave.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Reveal once when the clipped item enters the viewport, without changing its layout. */
internal fun Modifier.detailScrollReveal(key: String, image: Boolean = false): Modifier = composed {
    val motion = MotionAllowed()
    var entered by remember(key) { mutableStateOf(false) }
    val progress = remember(key) { Animatable(if (motion) 0f else 1f) }
    val distance = with(LocalDensity.current) { (if (image) 12.dp else 18.dp).toPx() }
    LaunchedEffect(entered, motion) {
        if (!motion) progress.snapTo(1f)
        else if (entered) progress.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
    }
    this.onGloballyPositioned { coordinates ->
        if (!entered) {
            val visible = coordinates.boundsInWindow()
            if (visible.width > 0f && visible.height > 0f) entered = true
        }
    }.graphicsLayer {
        val value = if (motion) progress.value else 1f
        alpha = .25f + .75f * value
        translationY = distance * (1f - value)
        if (image) {
            scaleX = .97f + .03f * value
            scaleY = scaleX
        }
    }
}
