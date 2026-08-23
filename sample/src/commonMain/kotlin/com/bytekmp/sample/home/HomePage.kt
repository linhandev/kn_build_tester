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

package com.bytekmp.sample.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import bytekmp_sample.sample.generated.resources.Res
import bytekmp_sample.sample.generated.resources.bytekmp
import com.bytekmp.sample.Colors
import com.bytekmp.sample.SamplePage
import com.bytekmp.sample.compoennts.BasicComponentPage
import com.bytekmp.sample.performance.PerformancePage
import com.bytekmp.sample.todo.TodoPage
import com.bytekmp.sample.todo.ui.TodoApp
import org.jetbrains.compose.resources.painterResource

object HomePage: SamplePage("Home") {
    @Composable
    override fun Content(navController: NavController) {
        Box(Modifier.fillMaxSize().padding(top = 40.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
                    .padding(40.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.bytekmp),
                    contentDescription = null,
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            HomePageItem("基础组件演示") {
                navController.navigate(BasicComponentPage.route)
            }
            HomePageInteropItem(navController)
            HomePagePerformanceItem(navController)
            HomePageLeakCanaryItem(navController)
            HomePageItem("Todo App") {
                navController.navigate(TodoPage.route)
            }
        }
    }
}

@Composable
expect fun HomePageInteropItem(navController: NavController)

@Composable
expect fun HomePagePerformanceItem(navController: NavController)

@Composable
expect fun HomePageLeakCanaryItem(navController: NavController)

@Composable
fun HomePageItem(text: String, onClick: () -> Unit,) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Colors.btn_bg,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable {
                    onClick()
                }
                .padding(vertical = 15.dp)
        )
        Spacer(Modifier.height(12.dp))
    }
}