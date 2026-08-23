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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.bytedance.kmp.compose.ohos.ArkTsExportComposable
import com.bytekmp.sample.SamplePage

/**
 * 全局静态单例，用于模拟外部长生命周期组件
 */
object GlobalEventManager {
    private val observers = mutableListOf<EventObserver>()

    fun register(observer: EventObserver) {
        observers.add(observer)
        println("Registered observer. Total: ${observers.size}")
    }

    fun unregister(observer: EventObserver) {
        observers.remove(observer)
        println("Unregistered observer. Total: ${observers.size}")
    }

    fun getObserverCount(): Int = observers.size
}

interface EventObserver {
    fun onEvent()
}

/**
 * 演示泄漏的 ViewModel
 */

class LeakViewModel : ViewModel(), EventObserver {
    var observerCount by mutableStateOf(GlobalEventManager.getObserverCount())

    init {
        // ViewModel 初始化时注册到全局单例
        GlobalEventManager.register(this)
        observerCount = GlobalEventManager.getObserverCount()
    }

    override fun onEvent() {
        println("Event received in LeakViewModel")
    }

    override fun onCleared() {
        super.onCleared()
        // 故意不在这里注销，导致 ViewModel 实例被 GlobalEventManager 永久持有
        // GlobalEventManager.unregister(this)
        println("LeakViewModel onCleared called (but NOT unregistering from GlobalEventManager)")
    }
}

object ComposeLeakPage : SamplePage("compose_leak", "ViewModel 内存泄漏演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        LeakContent()
    }
}
@ArkTsExportComposable("LeakContent")
@Composable
fun LeakContent() {
    // 在 Compose 中手动管理 ViewModel 生命周期（模拟简单场景）
    val viewModel = remember { LeakViewModel() }
    DisposableEffect(Unit) {
        println("ComposeLeakWatcher entered")
        onDispose {
            println("ComposeLeakWatcher onDispose")
        }
    }
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("此页面演示了 ViewModel 常见的内存泄漏场景：")
        Text("ViewModel 注册了全局单例的监听，但在销毁时没有注销。")
        Spacer(Modifier.height(20.dp))
        Text("当前全局监听器数量: ${viewModel.observerCount}")
        Text("(每次进入此页面都会创建一个新的 ViewModel 并泄漏)")
        Text("退出页面之后等 10s 会弹出检测泄漏的提示")
        Spacer(Modifier.height(20.dp))
//        Button(onClick = { navController.popBackStack() }) {
//            Text("返回并销毁 Page (触发泄漏)")
//        }
    }
}
