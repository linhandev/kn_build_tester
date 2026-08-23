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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SimpleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 20.dp
) {
    val borderColor = if (checked) Color(0xFF6200EE) else Color.Gray
    val backgroundColor = if (checked) Color(0xFF6200EE) else Color.Transparent

    Canvas(
        modifier = modifier
            .size(width)
            .clickable { onCheckedChange(!checked) }
    ) {
        val strokeWidth = 2.dp.toPx()

        // 画边框
        drawRoundRect(
            color = borderColor,
            style = Stroke(width = strokeWidth),
            cornerRadius = CornerRadius(4.dp.toPx())
        )

        // 画背景
        drawRoundRect(
            color = backgroundColor,
            cornerRadius = CornerRadius(4.dp.toPx())
        )

        // 画对勾
        if (checked) {
            val path = Path().apply {
                moveTo(size.width * 0.2f, size.height * 0.5f)
                lineTo(size.width * 0.45f, size.height * 0.75f)
                lineTo(size.width * 0.8f, size.height * 0.25f)
            }

            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
