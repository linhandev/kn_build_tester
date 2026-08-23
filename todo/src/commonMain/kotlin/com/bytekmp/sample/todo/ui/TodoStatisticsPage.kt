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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bytekmp.sample.todo.domain.TodoStatistics

@Composable
fun TodoStatisticsPage(
    viewModel: TodoStatisticsViewModel,
    onBackToDemo: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val stats by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBackToDemo) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Demo")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Task Overview",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (stats.isEmpty) {
                Text(
                    text = "No tasks found",
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray
                )
            } else {
                CombinedStatisticsContent(stats)
            }
        }
    }
}

@Composable
private fun CombinedStatisticsContent(stats: TodoStatistics) {
    val total = stats.activeTasks + stats.completedTasks
    val activePercentage = if (total > 0) stats.activeTasks.toFloat() / total else 0f
    val completedPercentage = if (total > 0) stats.completedTasks.toFloat() / total else 0f

    val activeColor = MaterialTheme.colors.primary
    val completedColor = Color(0xFF4CAF50)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Single Segmented Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            if (stats.activeTasks > 0) {
                Box(
                    modifier = Modifier
                        .weight(activePercentage)
                        .fillMaxHeight()
                        .background(activeColor)
                )
            }
            if (stats.completedTasks > 0) {
                Box(
                    modifier = Modifier
                        .weight(completedPercentage)
                        .fillMaxHeight()
                        .background(completedColor)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Legend / Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatLegendItem(
                label = "Active",
                count = stats.activeTasks,
                percentage = activePercentage,
                color = activeColor
            )
            StatLegendItem(
                label = "Completed",
                count = stats.completedTasks,
                percentage = completedPercentage,
                color = completedColor
            )
        }
    }
}

@Composable
private fun StatLegendItem(
    label: String,
    count: Int,
    percentage: Float,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$count tasks (${(percentage * 100).toInt()}%)",
            style = MaterialTheme.typography.body2,
            color = Color.Gray
        )
    }
}
