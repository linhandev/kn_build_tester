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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bytekmp.sample.SamplePage

object RichTextPage : SamplePage("BasicComponent-RichText", "Text-富文本演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        val scrollState = rememberScrollState()
        var lastClickedText by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // 1. 基础样式混合
            SectionTitle("1. 基础样式混合 (颜色/背景)")
            val basicRichText = buildAnnotatedString {
                append("这是一段普通文本，")
                withStyle(style = SpanStyle(color = Color.Red)) {
                    append("这是红色文本，")
                }
                withStyle(style = SpanStyle(color = Color.Blue)) {
                    append("这是蓝色文本，")
                }
                withStyle(style = SpanStyle(background = Color.Yellow)) {
                    append("这是黄色背景文本。")
                }
            }
            Text(text = basicRichText)

            // 2. 字体样式与装饰
            SectionTitle("2. 字体样式与装饰")
            val styleRichText = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("粗体文字 ")
                }
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append("斜体文字 ")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    append("粗斜体文字\n")
                }
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append("下划线文字 ")
                }
                withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append("删除线文字 ")
                }
                withStyle(style = SpanStyle(textDecoration = TextDecoration.combine(
                    listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                ))) {
                    append("组合装饰线")
                }
            }
            Text(text = styleRichText)

            // 3. 字号与字体
            SectionTitle("3. 字号与字体")
            val fontRichText = buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 24.sp)) {
                    append("大号文字 ")
                }
                withStyle(style = SpanStyle(fontSize = 12.sp)) {
                    append("小号文字\n")
                }
                withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append("等宽字体 (Monospace)\n")
                }
                withStyle(style = SpanStyle(fontFamily = FontFamily.Serif)) {
                    append("衬线字体 (Serif)\n")
                }
                withStyle(style = SpanStyle(
                    shadow = Shadow(
                        color = Color.Gray,
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )) {
                    append("带阴影的文字")
                }
            }
            Text(text = fontRichText)

            // 4. 可点击文本
            SectionTitle("4. 交互式文本 (点击效果)")
            
            val clickableText = buildAnnotatedString {
                append("试试点击")
                
                // 添加注解 tag
                pushStringAnnotation(tag = "ACTION", annotation = "这里")
                withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                    append("这里")
                }
                pop() // 结束 tag

                append("。\n或者")
                
                pushStringAnnotation(tag = "ACTION", annotation = "点击我试试看")
                withStyle(style = SpanStyle(
                    color = Color.White, 
                    background = Color(0xFF6200EE), // Purple
                    fontWeight = FontWeight.Bold
                )) {
                    append(" 点击我试试看 ")
                }
                pop()
            }

            ClickableText(
                text = clickableText,
                onClick = { offset ->
                    clickableText.getStringAnnotations(tag = "ACTION", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            lastClickedText = "你点击了：${annotation.item}"
                        }
                },
                style = MaterialTheme.typography.body1
            )
            
            if (lastClickedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lastClickedText,
                    color = Color(0xFF4CAF50), // Green
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
    }
}
