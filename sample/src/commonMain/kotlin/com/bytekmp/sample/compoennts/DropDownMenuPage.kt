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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bytekmp.sample.SamplePage

object DropDownMenuPage : SamplePage("BasicComponent-DropDownMenu", "DropDownMenu-下拉菜单演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. 基础按钮菜单
            item {
                SectionTitle("1. 基础按钮菜单")
                BasicDropdownMenu()
            }

            // 2. 图标菜单 (Context Menu)
            item {
                SectionTitle("2. 图标菜单 (更多选项)")
                IconDropdownMenu()
            }

            // 3. 模拟选择框 (TextField Dropdown)
            item {
                SectionTitle("3. 输入框下拉选择")
                TextFieldDropdownMenu()
            }

            // 4. 可滚动菜单
            item {
                SectionTitle("4. 长列表滚动菜单")
                ScrollableDropdownMenu()
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
    fun BasicDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }
        val items = listOf("选项 A", "选项 B", "选项 C")
        var selectedIndex by remember { mutableStateOf(0) }

        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            Button(onClick = { expanded = true }) {
                Text("当前选择: ${items[selectedIndex]}")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowDropDown, "DropDown")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEachIndexed { index, s ->
                    DropdownMenuItem(onClick = {
                        selectedIndex = index
                        expanded = false
                    }) {
                        Text(text = s)
                    }
                }
            }
        }
    }

    @Composable
    fun IconDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }

        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("刷新")
                }
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("设置")
                }
                Divider()
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("退出", color = Color.Red)
                }
            }
        }
    }

    @Composable
    fun TextFieldDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }
        val options = listOf("Kotlin", "Java", "Swift", "Dart", "Python")
        var selectedOption by remember { mutableStateOf("") }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { Text("选择编程语言") },
                placeholder = { Text("请选择") },
                trailingIcon = {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        "contentDescription",
                        Modifier.clickable { expanded = !expanded }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            // 透明层拦截点击
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { selection ->
                    DropdownMenuItem(onClick = {
                        selectedOption = selection
                        expanded = false
                    }) {
                        Text(text = selection)
                    }
                }
            }
        }
    }

    @Composable
    fun ScrollableDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }
        
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            OutlinedButton(onClick = { expanded = true }) {
                Text("打开长列表菜单")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                // DropdownMenu 内部默认支持滚动
                modifier = Modifier.heightIn(max = 200.dp) 
            ) {
                repeat(20) { index ->
                    DropdownMenuItem(onClick = { expanded = false }) {
                        Text("长列表选项 #$index")
                    }
                }
            }
        }
    }
}
