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

package com.bytekmp.sample.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.bytekmp.sample.compoennts.BasicComponentPage
import com.bytekmp.sample.interop.InteropViewPage
import com.bytekmp.sample.performance.ComposeLeakPage
import com.bytekmp.sample.performance.PerformancePage

@Composable
actual fun HomePageInteropItem(navController: NavController) {
    HomePageItem("原生组件混排演示") {
        navController.navigate(InteropViewPage.route)
    }
}

@Composable
actual fun HomePagePerformanceItem(navController: NavController) {
    HomePageItem("性能优化策略演示") {
        navController.navigate(PerformancePage.route)
    }
}

@Composable
actual fun HomePageLeakCanaryItem(navController: NavController) {
    HomePageItem("内存分析工具演示") {
        navController.navigate(ComposeLeakPage.route)
    }
}