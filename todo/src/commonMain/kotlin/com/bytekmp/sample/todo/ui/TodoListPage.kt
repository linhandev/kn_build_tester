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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bytekmp.sample.todo.domain.TodoItem
import com.bytekmp.sample.todo.domain.usecase.TodoFilterType

@Composable
fun TodoListPage(
    viewModel: TodoHomeViewModel,
    onItemClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onBackToDemo: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Todos") },
                navigationIcon = {
                    IconButton(onClick = onBackToDemo) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Demo")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    FilterMenu(
                        currentFilter = uiState.filter,
                        onFilterSelect = viewModel::setFilter,
                        onClearCompleted = viewModel::clearCompleted
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (uiState.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No todos found")
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    TodoItemRow(
                        item = item,
                        onMessageClick = { onItemClick(item.id) },
                        onToggle = { viewModel.toggleTodo(item) },
                        onDelete = { viewModel.deleteTodo(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterMenu(
    currentFilter: TodoFilterType,
    onFilterSelect: (TodoFilterType) -> Unit,
    onClearCompleted: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Filter")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(onClick = {
            onFilterSelect(TodoFilterType.ALL)
            expanded = false
        }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All",
                    color = if (currentFilter == TodoFilterType.ALL) 
                        MaterialTheme.colors.primary 
                    else 
                        MaterialTheme.colors.onSurface,
                    fontWeight = if (currentFilter == TodoFilterType.ALL) 
                        FontWeight.Bold 
                    else 
                        FontWeight.Normal
                )
                if (currentFilter == TodoFilterType.ALL) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        DropdownMenuItem(onClick = {
            onFilterSelect(TodoFilterType.ACTIVE)
            expanded = false
        }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Active",
                    color = if (currentFilter == TodoFilterType.ACTIVE) 
                        MaterialTheme.colors.primary 
                    else 
                        MaterialTheme.colors.onSurface,
                    fontWeight = if (currentFilter == TodoFilterType.ACTIVE) 
                        FontWeight.Bold 
                    else 
                        FontWeight.Normal
                )
                if (currentFilter == TodoFilterType.ACTIVE) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        DropdownMenuItem(onClick = {
            onFilterSelect(TodoFilterType.COMPLETED)
            expanded = false
        }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Completed",
                    color = if (currentFilter == TodoFilterType.COMPLETED) 
                        MaterialTheme.colors.primary 
                    else 
                        MaterialTheme.colors.onSurface,
                    fontWeight = if (currentFilter == TodoFilterType.COMPLETED) 
                        FontWeight.Bold 
                    else 
                        FontWeight.Normal
                )
                if (currentFilter == TodoFilterType.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Divider()
        DropdownMenuItem(onClick = {
            onClearCompleted()
            expanded = false
        }) {
            Text("Clear Completed")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoItemRow(
    item: TodoItem,
    onMessageClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onMessageClick,
                    onLongClick = { showMenu = true }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleCheckbox(
                checked = item.isDone,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.h6,
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else null
                )
                if (item.description.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.body2,
                        maxLines = 2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else null
                    )
                }
            }
        }
        
        if (showMenu) {
            AlertDialog(
                onDismissRequest = { showMenu = false },
                title = { Text("Delete Todo") },
                text = { Text("Are you sure you want to delete this specific task?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colors.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMenu = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
