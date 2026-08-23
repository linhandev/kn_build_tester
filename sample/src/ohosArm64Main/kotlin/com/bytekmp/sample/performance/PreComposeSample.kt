/*
 * Copyright (c) 2026 ByteDance Ltd. and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytekmp.sample.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.FrameObserver
import androidx.compose.ui.util.KPerfComposeConfig
import com.bytedance.kmp.compose.ohos.ArkTsExportComposable
import com.bytedance.kmp.ohos_ffi.annotation.KotlinExport
import com.bytedance.kmp.ohos_ffi.annotation.KotlinExportClass
import com.bytedance.kmp.ohos_ffi.annotation.KotlinExportClassGenerator
import com.bytedance.kmp.ohos_ffi.annotation.KotlinExportFunction
import com.bytedance.kmp.ohos_ffi.annotation.KotlinExportInterface
import com.bytedance.kmp.ohos_ffi.types.BigInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.random.Random


@KotlinExportInterface
interface PerformanceCallback {
    fun onDrawFrame(time: Long)
}

@KotlinExportClass
class PreComposeParam @KotlinExportClassGenerator constructor(val type: Int, val callback: PerformanceCallback?) {
    @KotlinExport
    val UUID: String = "$type"

    @KotlinExport
    fun getUUID(): String {
        return UUID
    }
}

@ArkTsExportComposable("PreComposeSample")
@Composable
fun PreComposeSample(param: PreComposeParam) {
    when (param.type) {
        0 -> NetworkUI(param)
        1 -> BlockUI(param)
    }
}


@ArkTsExportComposable("NormalComposeSample")
@Composable
fun NormalComposeSample(param: PreComposeParam) {
    when (param.type) {
        0 -> NetworkUI(param)
        1 -> BlockUI(param)
    }
}

@Composable
fun NetworkUI(param: PreComposeParam) {
    var data by remember { mutableStateOf<String?>(null) }
    var closeable: AutoCloseable? = remember { null }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Color.Yellow)
    ) {
        if (data == null) {
            CircularProgressIndicator()
        } else {
            Text(
                text = data!!,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    DisposableEffect(Unit) {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            // 模拟网络请求
            delay(3000)
            data = "Hello ByteKMP!"
            closeable = FrameObserver.subScribeFrameEnd {
                // precompose 阶段不上报
                if (it.launchTimeStampMs != -1L) {
                    param.callback?.onDrawFrame(Clock.System.now().toEpochMilliseconds())
                    closeable?.close()
                    closeable = null
                }
            }
        }
        onDispose {
            closeable?.close()
        }
    }
}

@Composable
fun BlockUI(param: PreComposeParam) {
    var closeable: AutoCloseable? = remember { null }

    // 模拟耗时
    fun block(time: Long) {
        runBlocking {
            delay(time)
        }
    }
    // 模拟 Compose 耗时
    block(300)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
            .background(Color.Yellow)
            .layout { measurable, constraints ->
                // 模拟 Layout 耗时
                block(300)
                val placeable = measurable.measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeRelative(
                        (constraints.maxWidth - placeable.width) / 2,
                        (constraints.maxHeight - placeable.height) / 2
                    )
                }
            }
            .drawWithContent {
                // 模拟 Draw 耗时
                block(300)
                drawContent()
            }
    ) {
        Text(
            text = "Hello ByteKMP!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
    DisposableEffect(Unit) {
        closeable = FrameObserver.subScribeFrameEnd {
            // precompose 阶段不上报
            if (it.launchTimeStampMs != -1L) {
                param.callback?.onDrawFrame(Clock.System.now().toEpochMilliseconds())
                closeable?.close()
                closeable = null
            }
        }
        onDispose {
            closeable?.close()
        }
    }
}

@KotlinExportFunction
fun enableFrameMonitor(enable: Boolean) {
    KPerfComposeConfig.enableFrameMonitor = enable
}
