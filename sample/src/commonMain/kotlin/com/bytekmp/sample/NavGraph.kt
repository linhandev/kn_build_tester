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

package com.bytekmp.sample

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bytedance.kmp.compose.ohos.ArkTsExportComposable
import com.bytekmp.sample.home.HomePage
import com.bytekmp.sample.compoennts.BasicComponentPage
import com.bytekmp.sample.performance.PerformancePage
import com.bytekmp.sample.todo.TodoPage

private val pageList = listOf(
    HomePage,
    PerformancePage,
    TodoPage
) + BasicComponentPage.getPages() + platformPageList()

expect fun platformPageList(): List<SamplePage>

@Composable
fun NavGraph(modifier: Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        startDestination = HomePage.route
    ) {
        pageList.forEach { page ->
            composable(page.route) {
                page(navController)
            }
        }
    }
}