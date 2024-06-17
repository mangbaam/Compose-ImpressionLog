package com.mangbaam.impressionlog

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mangbaam.impressionlog.impression.DefaultImpressionState
import com.mangbaam.impressionlog.impression.ImpressionState
import com.mangbaam.impressionlog.impression.LocalImpressionState
import com.mangbaam.impressionlog.impression.ProvideImpressionState
import com.mangbaam.impressionlog.impression.onExposed
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = {
                    Text("delay: 1000ms, ratio: 0.7")
                },
            )
        },
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
                        /*Content(
                            key = content.toString(),
                            contentHeight = (content * 10).dp,
                        ) {
                            ratio[content] = it
                        }*/
                        HorizontalList(
                            key = content.toString(),
                            contentHeight = (content * 20).dp,
                        ) {}
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

@Composable
fun HorizontalList(
    key: String,
    contentHeight: Dp,
    modifier: Modifier = Modifier,
    onChangeRatio: (ratio: Float) -> Unit,
) {
    val scrollState = rememberLazyListState()
    val keys = List(10) { "$key-${it + 1}" }

    LazyRow(
        modifier = modifier
            .impression(key = key, onChangeRatio = onChangeRatio)
            .height(contentHeight),
        state = scrollState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        keys.forEach { key ->
            item {
                HorizontalListItem(
                    modifier = Modifier.fillParentMaxHeight(),
                    key = key,
                    height = contentHeight,
                )
            }
        }
    }
}

@Composable
fun HorizontalListItem(
    key: String,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    var ratio by remember { mutableFloatStateOf(0f) }
    var impressed by remember { mutableStateOf(false) }
    val color by remember {
        derivedStateOf {
            if (impressed) Color.Green else Color.Red
        }
    }
    val impressionState = LocalImpressionState.current ?: rememberImpressionState()

    LaunchedEffect(Unit) {
        (impressionState as DefaultImpressionState).impressedItems.collect {
            impressed = it.contains(key)
        }
    }

    Box(
        modifier = modifier
            .impression(
                key = key,
                delayTimeMs = 500,
                ratio = 0.7f,
                onImpression = { impressed = true },
            )
            .onExposed {
                ratio = it
            }
            .width(height)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ratio.toString(),
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
