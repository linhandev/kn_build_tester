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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxHeight()
        .fillMaxWidth()
        .background(MaterialTheme.colors.surface)
    ) {
        // Drawer Header
        DrawerHeader()
        
        DrawerItem(
            icon = Icons.Default.List,
            label = "Todo List",
            isSelected = currentRoute == "home",
            action = {
                onNavigate("home")
                closeDrawer()
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        DrawerItem(
            icon = Icons.Default.Info,
            label = "Statistics",
            isSelected = currentRoute == "statistics",
            action = {
                onNavigate("statistics")
                closeDrawer()
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "v1.0.0",
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
            color = Color.Gray
        )
    }
}



@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colors
    val backgroundColor = if (isSelected) {
        colors.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        colors.primary
    } else {
        colors.onSurface.copy(alpha = 0.7f)
    }

    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = action)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor
        )
        Spacer(Modifier.width(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.subtitle1,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colors.primary,
                        MaterialTheme.colors.primary.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Todo App",
                style = MaterialTheme.typography.h5,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Based on ByteKMP",
                style = MaterialTheme.typography.body2,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
