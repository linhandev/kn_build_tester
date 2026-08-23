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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.bytekmp.sample.SamplePage

object DialogPage : SamplePage("BasicComponent-Dialog", "Dialog-弹窗演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. 标准 AlertDialog
            item {
                SectionTitle("1. 标准警告弹窗 (AlertDialog)")
                AlertDialogDemo()
            }

            // 2. 确认/取消弹窗
            item {
                SectionTitle("2. 确认操作弹窗")
                ConfirmDialogDemo()
            }

            // 3. 自定义内容 Dialog
            item {
                SectionTitle("3. 自定义内容弹窗 (Dialog)")
                CustomContentDialogDemo()
            }
            
            // 4. 不可取消弹窗
            item {
                SectionTitle("4. 强制操作弹窗 (不可点击外部取消)")
                NonDismissibleDialogDemo()
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

    @Composable
    fun AlertDialogDemo() {
        var showDialog by remember { mutableStateOf(false) }

        Button(onClick = { showDialog = true }) {
            Text("显示普通弹窗")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "标题") },
                text = { Text(text = "这是一个标准的 AlertDialog，用于提示用户信息。") },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }
    }

    @Composable
    fun ConfirmDialogDemo() {
        var showDialog by remember { mutableStateOf(false) }
        var resultText by remember { mutableStateOf("") }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showDialog = true }) {
                Text("删除确认")
            }
            if (resultText.isNotEmpty()) {
                Text(text = resultText, style = MaterialTheme.typography.caption, color = Color.Gray)
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "确认删除？") },
                text = { Text(text = "此操作不可恢复，请谨慎操作。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            resultText = "已执行删除操作"
                        }
                    ) {
                        Text("删除", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            resultText = "已取消"
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }

    @Composable
    fun CustomContentDialogDemo() {
        var showDialog by remember { mutableStateOf(false) }

        Button(onClick = { showDialog = true }) {
            Text("显示自定义弹窗")
        }

        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                // 自定义弹窗内容容器
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "自定义布局",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在处理中...", style = MaterialTheme.typography.body2)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { showDialog = false }) {
                            Text("关闭")
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun NonDismissibleDialogDemo() {
        var showDialog by remember { mutableStateOf(false) }

        Button(onClick = { showDialog = true }) {
            Text("显示强制弹窗")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { 
                    // 空实现，禁止点击外部关闭
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text(text = "强制更新") },
                text = { Text(text = "检测到新版本，您必须更新才能继续使用。") },
                confirmButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("立即更新")
                    }
                }
            )
        }
    }
}
