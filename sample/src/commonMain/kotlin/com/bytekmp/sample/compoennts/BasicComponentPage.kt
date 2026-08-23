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

package com.bytekmp.sample.compoennts

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
import com.bytekmp.sample.SamplePage

val componentPageList = listOf<SamplePage>(
    TextFontPage,
    RichTextPage,
    ImagePage,
    TextFieldPage,
    DropDownMenuPage,
    DialogPage
)

object BasicComponentPage: SamplePage("BasicComponent", "基础组件演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        LazyColumn(Modifier.fillMaxSize().padding(10.dp)) {
            items(componentPageList) { item->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 15.dp)
                        .clickable {
                            navController.navigate(item.route)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.title)
                }
            }
        }
    }

    fun getPages(): List<SamplePage> {
        return componentPageList + this
    }

}
