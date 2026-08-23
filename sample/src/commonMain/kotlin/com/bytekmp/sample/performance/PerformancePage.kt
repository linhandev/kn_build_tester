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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bytedance.kmp.spi.kmpService
import com.bytekmp.sample.SamplePage
import com.bytekmp.sample.compoennts.DialogPage
import com.bytekmp.sample.compoennts.DropDownMenuPage
import com.bytekmp.sample.compoennts.ImagePage
import com.bytekmp.sample.compoennts.RichTextPage
import com.bytekmp.sample.compoennts.TextFieldPage
import com.bytekmp.sample.compoennts.TextFontPage

object PerformancePage: SamplePage("Performance", "性能优化策略演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        val preComposeSample = kmpService<IPreComposeSample>()

        LazyColumn(Modifier.fillMaxSize().padding(10.dp)) {
            if (preComposeSample?.enablePreCompose() == true) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 15.dp)
                            .clickable {
                                preComposeSample.openPreComposePage()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("PreCompose 演示")
                    }
                }
            }
        }
    }
}
