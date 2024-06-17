package com.mangbaam.impressionlog.impression

import android.graphics.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView

fun Modifier.onExposed(
    onExposed: (ratio: Float) -> Unit,
): Modifier = composed {
    val view = LocalView.current
    onGloballyPositioned { cooridnates ->
        val boundsInWindow = cooridnates.boundsInWindow()
        val globalRootRect = Rect()
        view.getGlobalVisibleRect(globalRootRect)
        val composeViewRect = globalRootRect.toComposeRect()

        val top = maxOf(boundsInWindow.top, composeViewRect.top)
        val bottom = minOf(boundsInWindow.bottom, composeViewRect.bottom)
        val visibleHeight = bottom - top
        if (visibleHeight < 0) return@onGloballyPositioned run {
            onExposed(0f)
        }

        val left = maxOf(boundsInWindow.left, composeViewRect.left)
        val right = minOf(boundsInWindow.right, composeViewRect.right)
        val visibleWidth = right - left
        if (visibleWidth < 0) return@onGloballyPositioned run {
            onExposed(0f)
        }

        val componentArea = cooridnates.size.run { width * height }
        val visibleArea = visibleWidth * visibleHeight

        val ratio = visibleArea / componentArea
        onExposed(ratio)
    }
}
