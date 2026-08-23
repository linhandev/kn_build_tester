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

@file:OptIn(ExperimentalForeignApi::class)

package com.bytekmp.sample.interop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.ArkUIView
import androidx.compose.ui.interop.arkui.builtin.ArkUIStack
import androidx.compose.ui.interop.arkui.custom.ArkUIComponentContent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bytedance.kmp.ohos_ffi.annotation.KotlinExport
import com.bytedance.kmp.ohos_ffi.annotation.KotlinExportClass
import com.bytedance.kmp.ohos_ffi.annotation.KotlinExportInterface
import com.bytedance.kmp.ohos_ffi.types.ArkInstance
import com.bytedance.kmp.spi.IKmpService
import com.bytedance.kmp.spi.annotation.ArkTsSpiClass
import com.bytedance.kmp.spi.kmpService
import com.bytekmp.sample.SamplePage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.ohos.napi.napi_value


@KotlinExportClass
class ArkUIWithCallback {
    var componentContent: ArkInstance? = null

    var callback: ICallback? = null

    @KotlinExport
    fun setComponentContent(componentContent: napi_value) {
        this.componentContent = ArkInstance(componentContent)
    }

    @KotlinExport
    fun addCallback(callback: ICallback) {
        this.callback = callback
    }
}

@KotlinExportInterface
interface ICallback {
    fun onInputUpdate(input: String)
}

@ArkTsSpiClass("@kmp/ability_impl")
@KotlinExportInterface
interface IArkUIProvider: IKmpService {
    fun getArkUIComponentContent(): ArkUIWithCallback
}

object CustomInteropPage: SamplePage("InteropView-Custom", "自定义 ArkUI 混排", true) {
    @Composable
    override fun Content(navController: NavController) {
        val arkui = remember { kmpService<IArkUIProvider>()?.getArkUIComponentContent() } ?: return
        val componentContent = remember { ArkUIComponentContent(arkui.componentContent!!.getNapiValue(), true) }

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var input by remember { mutableStateOf("") }
            Row {
                TextField(
                    value = input,
                    onValueChange = {
                        input = it
                    },
                    label = {
                        Text("Compose TextField")
                    },
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        arkui.callback?.onInputUpdate(input)
                        input = ""
                    }
                ) {
                    Text("Add to ArkUI")
                }
            }
            ArkUIView(
                factory = { componentContent },
                modifier = Modifier.fillMaxWidth().weight(1f),
                onRelease = {
                    it.dispose()
                }
            )
        }
    }
}