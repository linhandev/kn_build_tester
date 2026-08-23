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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

abstract class SamplePage(val route: String, val title: String = "", val canBack: Boolean = false) {

    @Composable
    abstract fun Content(navController: NavController)

    @Composable
    operator fun invoke(navController: NavController) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Color.White)
        ) {
            if (title.isNotEmpty()) {
                TitleBar(canBack) {
                    navController.popBackStack()
                }
            }
            Box(
                Modifier.fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Content(navController)
            }
        }
        if (canBack) {
            OnBack {
                navController.popBackStack()
            }
        }
    }

    @Composable
    private fun TitleBar(canBack: Boolean, onBack: () -> Unit) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                )
                if (canBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.clickable { onBack() }
                            .padding(16.dp)
                            .align(Alignment.CenterStart)
                    )
                }
            }
            Divider(
                color = Color.LightGray,
                modifier = Modifier.height(0.5.dp),
            )
        }
    }

}

@Composable
expect fun OnBack(onBack: () -> Unit)