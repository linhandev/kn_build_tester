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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import bytekmp_sample.sample.generated.resources.JetBrainsMono
import bytekmp_sample.sample.generated.resources.Res
import com.bytekmp.sample.SamplePage
import org.jetbrains.compose.resources.Font

class TextStyleUnit(val name: String, val textStyle: TextStyle)

object TestStyles {
    val H0 = TextStyle.Default.copy(
        fontSize = 32.sp,
        lineHeight = 40.sp,
    )
    val H1 = TextStyle.Default.copy(
        fontSize = 28.sp,
        lineHeight = 36.sp,
    )
    val H2 = TextStyle.Default.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
    )
    val H3 = TextStyle.Default.copy(
        fontSize = 20.sp,
        lineHeight = 28.sp,
    )
    val H4 = TextStyle.Default.copy(
        fontSize = 18.sp,
        lineHeight = 26.sp,
    )
    val P1 = TextStyle.Default.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val P2 = TextStyle.Default.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val P3 = TextStyle.Default.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val T1 = TextStyle.Default.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )
    val T2 = TextStyle.Default.copy(
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )
}

val styles = listOf(
    TextStyleUnit("H0",TestStyles.H0),
    TextStyleUnit("H1",TestStyles.H1),
    TextStyleUnit("H2",TestStyles.H2),
    TextStyleUnit("H3",TestStyles.H3),
    TextStyleUnit("H4",TestStyles.H4),

    TextStyleUnit("P1",TestStyles.P1),
    TextStyleUnit("P2",TestStyles.P2),
    TextStyleUnit("P3",TestStyles.P3),

    TextStyleUnit("T1",TestStyles.T1),
    TextStyleUnit("T2",TestStyles.T2),
)

object TextFontPage: SamplePage("BasicComponent-Text-Font", "Text-字体演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        Row(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("系统字体")
                styles.forEach {
                    Text(
                        text = it.name,
                        style = it.textStyle
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("特殊字体")
                styles.forEach {
                    Text(
                        text = it.name,
                        style = it.textStyle,
                        fontFamily = FontFamily(Font(Res.font.JetBrainsMono))
                    )
                }
            }
        }
    }
}