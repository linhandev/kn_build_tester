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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bytekmp_sample.sample.generated.resources.Res
import bytekmp_sample.sample.generated.resources.bytekmp
import com.bytekmp.sample.SamplePage
import org.jetbrains.compose.resources.painterResource

object ImagePage : SamplePage("BasicComponent-Image", "Image-图片演示", true) {
    @Composable
    override fun Content(navController: NavController) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Image ContentScale 展示",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ContentScale.Crop
            item {
                ImageExample(
                    title = "ContentScale.Crop",
                    description = "裁剪图片以填充容器，保持宽高比",
                    contentScale = ContentScale.Crop
                )
            }

            // ContentScale.Fit
            item {
                ImageExample(
                    title = "ContentScale.Fit",
                    description = "缩放图片以适应容器，保持宽高比，至少一边贴合",
                    contentScale = ContentScale.Fit
                )
            }

            // ContentScale.FillBounds
            item {
                ImageExample(
                    title = "ContentScale.FillBounds",
                    description = "拉伸图片以填满容器，不保持宽高比",
                    contentScale = ContentScale.FillBounds
                )
            }

            // ContentScale.FillWidth
            item {
                ImageExample(
                    title = "ContentScale.FillWidth",
                    description = "缩放图片使宽度填满容器，保持宽高比",
                    contentScale = ContentScale.FillWidth
                )
            }

            // ContentScale.FillHeight
            item {
                ImageExample(
                    title = "ContentScale.FillHeight",
                    description = "缩放图片使高度填满容器，保持宽高比",
                    contentScale = ContentScale.FillHeight
                )
            }

            // ContentScale.Inside
            item {
                ImageExample(
                    title = "ContentScale.Inside",
                    description = "如果图片大于容器则缩放至适应，否则保持原大小",
                    contentScale = ContentScale.Inside
                )
            }

            // ContentScale.None
            item {
                ImageExample(
                    title = "ContentScale.None",
                    description = "不缩放，保持原始大小，居中显示",
                    contentScale = ContentScale.None
                )
            }

            item {
                Text(
                    text = "其他特性展示",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // Clip / Shape
            item {
                Column {
                    Text(
                        text = "Clip (CircleShape)",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .border(1.dp, Color.Gray, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.bytekmp),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }
            
            // Alpha
             item {
                Column {
                    Text(
                        text = "Alpha (0.5f)",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(200.dp, 150.dp)
                            .border(1.dp, Color.Gray)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.bytekmp),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.5f)
                        )
                    }
                }
            }

            // ColorFilter
            item {
                Column {
                    Text(
                        text = "ColorFilter (Tint Red)",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(200.dp, 150.dp)
                            .border(1.dp, Color.Gray)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.bytekmp),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color.Red),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    @Composable
    private fun ImageExample(
        title: String,
        description: String,
        contentScale: ContentScale
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .size(200.dp, 150.dp) // Fixed container size to demonstrate scaling
                    .border(1.dp, Color.LightGray)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.bytekmp),
                    contentDescription = title,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
