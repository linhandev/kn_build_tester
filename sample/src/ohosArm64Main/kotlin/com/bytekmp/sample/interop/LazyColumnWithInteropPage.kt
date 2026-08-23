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

package com.bytekmp.sample.interop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.ArkUIView
import androidx.compose.ui.interop.ArkUIViewHitTestMode
import androidx.compose.ui.interop.arkui.builtin.ArkUIButton
import androidx.compose.ui.interop.arkui.builtin.ArkUIStack
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bytedance.kmp.compose.ohos.HitTestMode
import com.bytekmp.sample.SamplePage

object LazyColumnWithInteropPage: SamplePage("InteropView-LazyColumn", "LazyColumn + ArkUI", true) {
    @Composable
    override fun Content(navController: NavController) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(100) {
                if (it % 2 == 0) {
                    ArkUIView(
                        factory = {
                            val button = ArkUIButton()
                            button.setText("ArkUI Button $it")
                            button
                        },
                        hitTestMode = { ArkUIViewHitTestMode.TRANSPARENT },
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    )
                } else {
                    Text("Compose Text $it", Modifier.height(40.dp))
                }
            }
        }
    }
}