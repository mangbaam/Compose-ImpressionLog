package com.mangbaam.impressionlog.impression

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Composable
fun rememberImpressionState(key: Any? = null): ImpressionState {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(key ?: false) { DefaultImpressionState(lifecycleOwner.lifecycle) }
}

@Composable
fun ProvideImpressionState(state: ImpressionState, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalImpressionState provides state, content = content)
}

val LocalImpressionState = compositionLocalOf<ImpressionState?> { null }

interface ImpressionState {
    val impressionFlow: Flow<ImpressionItem>

    fun onLayoutChanged(
        item: ImpressionItem,
        size: IntSize,
        boundsRect: Rect,
        composeViewRect: Rect,
        onRatioChanged: (ratio: Float) -> Unit = {},
    )

    fun onDispose(item: ImpressionItem)

    fun clearCache()
}

data class ImpressionItem(
    val key: Any,
    val delayTimeMs: Long = DEFAULT_DELAY_TIME_MS,
    val ratio: Float = DEFAULT_RATIO,
    val observeStartTime: () -> Long = { System.currentTimeMillis() },
) {
    companion object {
        const val DEFAULT_DELAY_TIME_MS = 2000L
        const val DEFAULT_RATIO = 0.5f
    }
}

class DefaultImpressionState(
    scope: (block: suspend CoroutineScope.() -> Unit) -> Unit,
    updateInterval: Long = UPDATE_INTERVAL_MS,
) : ImpressionState {

    constructor(
        lifecycle: Lifecycle,
        updateInterval: Long = UPDATE_INTERVAL_MS,
    ) : this(
        scope = { block ->
            lifecycle.coroutineScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { block() }
            }
        },
        updateInterval = updateInterval,
    )

    data class VisibleItem(val item: ImpressionItem, val startTime: Long)

    private val _impressionFlow = MutableSharedFlow<ImpressionItem>()
    override val impressionFlow: Flow<ImpressionItem> = _impressionFlow.asSharedFlow()

    private val _visibleItems: ConcurrentMap<Any, VisibleItem> = ConcurrentHashMap()
    val visibleItems: Map<Any, VisibleItem>
        get() = _visibleItems.toMap()

    private val _impressedItems: MutableStateFlow<HashSet<Any>> = MutableStateFlow(hashSetOf())
    val impressedItems: StateFlow<Set<Any>> = _impressedItems.asStateFlow()

    init {
        Log.d("MANGBAAM_State", "ImpressionState: init called")
        scope {
            while (true) {
                _visibleItems.values
                    .asSequence()
                    // check already impressed
                    .filterNot { visibleItem ->
                        impressedItems.value.contains(visibleItem.item.key)
                    }
                    // check delay time
                    .filter { visibleItem ->
                        visibleItem.startTime + visibleItem.item.delayTimeMs <= visibleItem.item.observeStartTime()
                    }
                    .forEach { visibleItem ->
                        _impressedItems.update {
                            it.apply { add(visibleItem.item.key) }
                        }
                        _impressionFlow.emit(visibleItem.item)
                    }
                delay(updateInterval)
            }
        }
    }

    override fun clearCache() {
        _impressedItems.update { hashSetOf() }
    }

    override fun onLayoutChanged(
        item: ImpressionItem,
        size: IntSize,
        boundsRect: Rect,
        composeViewRect: Rect,
        onRatioChanged: (ratio: Float) -> Unit,
    ) {
        if (impressedItems.value.any { it == item.key }) return

        val top = maxOf(boundsRect.top, composeViewRect.top)
        val bottom = minOf(boundsRect.bottom, composeViewRect.bottom)
        val visibleHeight = bottom - top
        if (visibleHeight < 0) return run {
            onRatioChanged(0f)
            onDispose(item)
        }

        val left = maxOf(boundsRect.left, composeViewRect.left)
        val right = minOf(boundsRect.right, composeViewRect.right)
        val visibleWidth = right - left
        if (visibleWidth < 0) return run {
            onRatioChanged(0f)
            onDispose(item)
        }

        val componentArea = size.width * size.height
        val visibleArea = visibleWidth * visibleHeight

        val ratio = visibleArea / componentArea
        onRatioChanged(ratio)

        if (ratio >= item.ratio) {
            _visibleItems.any { it.key == item.key }.let {
                if (!it) _visibleItems[item.key] = VisibleItem(item, item.observeStartTime())
            }
        } else {
            onDispose(item)
        }
    }

    override fun onDispose(item: ImpressionItem) {
        _visibleItems.remove(item.key)
    }

    companion object {
        const val UPDATE_INTERVAL_MS = 10L
    }
}
