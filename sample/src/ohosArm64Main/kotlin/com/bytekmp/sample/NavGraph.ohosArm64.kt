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

package com.bytekmp.sample

import androidx.compose.runtime.LaunchedEffect
import com.bytedance.kmp.leakwatcher.core.ILeakDetectCallbacks
import com.bytedance.kmp.leakwatcher.core.LeakDetectResult
import com.bytedance.kmp.leakwatcher.core.LeakObjectInfo
import com.bytedance.kmp.leakwatcher.watcher.ComposeLeakWatcher
import com.bytedance.kmp.leakwatcher.watcher.ComposeLeakWatcherConfig
import com.bytekmp.sample.interop.InteropViewPage
import com.bytekmp.sample.performance.ComposeLeakPage

actual fun platformPageList(): List<SamplePage> {
    ComposeLeakWatcher.DEFAULT_WATCHER.start(ComposeLeakWatcherConfig(threshold = 1))
    ComposeLeakWatcher.DEFAULT_WATCHER.registerLeakDetectCallbacks(object : ILeakDetectCallbacks {
        override fun onDetectCompleted(result: LeakDetectResult) {

        }

        override fun onDetectFailed(tag: String, reason: String) {
        }

        override fun onDetected(leakInfos: List<LeakObjectInfo>) {
            val names = leakInfos.joinToString { it.className.substringAfterLast('.') }
            ToastUtil.show("检测到内存泄漏: $names")
        }
    })
    return InteropViewPage.getPages() + ComposeLeakPage
}