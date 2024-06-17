package com.mangbaam.impressionlog

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mangbaam.impressionlog.impression.DefaultImpressionState
import com.mangbaam.impressionlog.impression.ImpressionState
import com.mangbaam.impressionlog.impression.LocalImpressionState
import com.mangbaam.impressionlog.impression.ProvideImpressionState
import com.mangbaam.impressionlog.impression.impression
import com.mangbaam.impressionlog.impression.rememberImpressionState
import com.mangbaam.impressionlog.ui.theme.ImpressionLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImpressionLogTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Main(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Main(modifier: Modifier = Modifier) {
    val contents = List(100) { it + 1 }
    val ratio = remember { mutableStateMapOf<Int, Float>() }

    val impressionState = rememberImpressionState("Main")

    LaunchedEffect(impressionState) {
        impressionState.impressionFlow.collect {
            Log.d("MANGBAAM_IMPL", "Main impression: $it")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { impressionState.clearCache() },
            ) {
                Text("초기화")
            }
        },
    ) { innerPadding ->
        ProvideImpressionState(impressionState) {
            LazyColumn(modifier = modifier.padding(innerPadding)) {
                contents.forEach { content ->
                    stickyHeader {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            text = "Height: ${content * 10}, ratio: ${
                                ratio.getOrDefault(
                                    content,
                                    0f
                                )
                            }",
                        )
                    }
                    item {
                        Content(
                            key = content.toString(),
                            contentHeight = (content * 10).dp,
                        ) {
                            ratio[content] = it
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Content(
    key: String,
    contentHeight: Dp,
    modifier: Modifier = Modifier,
    impressionState: ImpressionState = LocalImpressionState.current ?: rememberImpressionState(),
    onChangeRatio: (ratio: Float) -> Unit,
) {
    var impressed by remember { mutableStateOf(false) }
    val color = if (impressed) Color.Green else Color.Red

    LaunchedEffect(Unit) {
        (impressionState as DefaultImpressionState).impressedItems.collect {
            impressed = it.contains(key)
        }
    }

    Column(modifier = modifier) {
        val rect = android.graphics.Rect()
        LocalView.current.getGlobalVisibleRect(rect)
        Box(
            modifier = Modifier
                .impression(
                    key = key,
                    onChangeRatio = onChangeRatio,
                    ratio = 0.7f,
                    delayTimeMs = 1000L,
                ) {
                    impressed = true
                }
                .fillMaxWidth()
                .height(contentHeight)
                .background(color),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    ImpressionLogTheme {
        Main()
    }
}
