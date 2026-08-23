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

package com.bytekmp.sample.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TodoDetailPage(
    viewModel: TodoDetailViewModel,
    onEdit: (String) -> Unit,
    onBack: () -> Unit
) {
    val item by viewModel.item.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()

    if (isDeleted) {
        LaunchedEffect(Unit) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todo Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::deleteTodo) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        floatingActionButton = {
            item?.let {
                FloatingActionButton(onClick = { onEdit(it.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item?.let { todo ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SimpleCheckbox(
                                    checked = todo.isDone,
                                    onCheckedChange = { viewModel.toggleStatus() }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Mark as ${if (todo.isDone) "active" else "completed"}",
                                    style = MaterialTheme.typography.body1
                                )
                            }
                            
                            // Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (todo.isDone) 
                                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        else 
                                            MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (todo.isDone) "Completed" else "Active",
                                    style = MaterialTheme.typography.caption,
                                    color = if (todo.isDone) 
                                        Color(0xFF4CAF50)
                                    else 
                                        MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Content Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Title Section
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Title",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = todo.title,
                                    style = MaterialTheme.typography.h5,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Description Section
                            if (todo.description.isNotEmpty()) {
                                Divider()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Description",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = todo.description,
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.87f)
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
