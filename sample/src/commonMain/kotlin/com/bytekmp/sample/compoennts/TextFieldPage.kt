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

@file:OptIn(ExperimentalComposeUiApi::class)

package com.bytekmp.sample.compoennts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bytekmp.sample.SamplePage

object TextFieldPage : SamplePage("BasicComponent-TextField", "TextField-输入框演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        val focusManager = LocalFocusManager.current
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. 基础样式
            item {
                SectionTitle("1. 基础样式 (Filled vs Outlined)")
                var text1 by remember { mutableStateOf("") }
                var text2 by remember { mutableStateOf("") }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextField(
                        value = text1,
                        onValueChange = { text1 = it },
                        label = { Text("标准输入框 (Filled)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = text2,
                        onValueChange = { text2 = it },
                        label = { Text("轮廓输入框 (Outlined)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. 装饰图标与提示
            item {
                SectionTitle("2. 装饰图标与提示")
                var text by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("用户名") },
                    placeholder = { Text("请输入用户名") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    trailingIcon = { 
                        if (text.isNotEmpty()) {
                            IconButton(onClick = { text = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. 密码输入 (VisualTransformation)
            item {
                SectionTitle("3. 密码输入 (显/隐切换)")
                var password by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.Lock else Icons.Filled.Info
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = if (passwordVisible) "Hide Password" else "Show Password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. 错误状态
            item {
                SectionTitle("4. 错误状态")
                var text by remember { mutableStateOf("") }
                val isError = text.length > 5

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("最多输入5个字符") },
                    isError = isError,
                    trailingIcon = {
                        if (isError) Icon(Icons.Filled.Warning, "Error", tint = MaterialTheme.colors.error)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "输入过长！",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
    }
}
