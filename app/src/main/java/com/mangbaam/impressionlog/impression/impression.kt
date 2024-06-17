package com.mangbaam.impressionlog.impression

import android.graphics.Rect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.mangbaam.impressionlog.impression.ImpressionItem.Companion.DEFAULT_DELAY_TIME_MS
import com.mangbaam.impressionlog.impression.ImpressionItem.Companion.DEFAULT_RATIO

fun Modifier.impression(
    key: Any,
    delayTimeMs: Long = DEFAULT_DELAY_TIME_MS,
    ratio: Float = DEFAULT_RATIO,
    impressionState: ImpressionState? = null,
    onChangeRatio: (ratio: Float) -> Unit = {},
    onImpression: (item: ImpressionItem) -> Unit = {},
): Modifier = composed(
    inspectorInfo = { properties["key"] = key },
) {
    val state = impressionState ?: LocalImpressionState.current ?: rememberImpressionState()
    val item = ImpressionItem(key, delayTimeMs, ratio)

    LaunchedEffect(key) {
        state.impressionFlow.collect {
            if (key == it.key) onImpression(item)
        }
    }

    DisposableEffect(key) {
        onDispose {
            state.onDispose(item)
        }
    }

    val view = LocalView.current
    onGloballyPositioned { coordinates ->
        val boundsInWindow = coordinates.boundsInWindow()
        val globalRootRect = Rect()
        view.getGlobalVisibleRect(globalRootRect)
        state.onLayoutChanged(
            item, coordinates.size, boundsInWindow, globalRootRect.toComposeRect(), onChangeRatio,
        )
    }
}
