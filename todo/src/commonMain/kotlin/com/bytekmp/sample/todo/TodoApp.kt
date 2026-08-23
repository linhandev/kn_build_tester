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

import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bytekmp.sample.todo.data.TodoDataStore
import com.bytekmp.sample.todo.domain.usecase.*
import kotlinx.coroutines.launch

// Define Navigation Routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Editor : Screen("editor?todoId={todoId}") {
        fun createRoute(todoId: String? = null) = if (todoId != null) "editor?todoId=$todoId" else "editor"
    }
    object Detail : Screen("detail/{todoId}") {
        fun createRoute(todoId: String) = "detail/$todoId"
    }
    object Statistics : Screen("statistics")
}

@Composable
fun TodoApp(
    parentNavController: NavController,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    // Manual Dependency Injection
    // 1. Data Layer
    val todoDataStore = remember { TodoDataStore() }
    
    // 2. Domain Layer (UseCases)
    val getTodosUseCase = remember { GetTodosUseCase(todoDataStore) }
    val getTodoItemUseCase = remember { GetTodoItemUseCase(todoDataStore) }
    val saveTodoUseCase = remember { SaveTodoUseCase(todoDataStore) }
    val toggleTodoStatusUseCase = remember { ToggleTodoStatusUseCase(todoDataStore) }
    val deleteTodoUseCase = remember { DeleteTodoUseCase(todoDataStore) }
    val clearCompletedUseCase = remember { ClearCompletedUseCase(todoDataStore) }
    val getStatisticsUseCase = remember { GetStatisticsUseCase(todoDataStore) }

    // Logic to sync navigation with drawer selection
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }
    
    // We wrap the whole NavHost in a Scaffold to provide the global Drawer
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    if (route == "home") {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    } else if (route == "statistics") {
                        navController.navigate(Screen.Statistics.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                },
                closeDrawer = {
                    scope.launch { scaffoldState.drawerState.close() }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier
        ) {
            // Home Screen
            composable(Screen.Home.route) {
                currentRoute = "home"
                val viewModel = remember {
                    TodoHomeViewModel(
                        getTodosUseCase,
                        toggleTodoStatusUseCase,
                        clearCompletedUseCase,
                        deleteTodoUseCase
                    )
                }
                TodoListPage(
                    viewModel = viewModel,
                    onItemClick = { todoId ->
                        navController.navigate(Screen.Detail.createRoute(todoId))
                    },
                    onAddClick = {
                        navController.navigate(Screen.Editor.createRoute(null))
                    },
                    onBackToDemo = {
                        parentNavController.popBackStack()
                    },
                    onOpenDrawer = { 
                         scope.launch { scaffoldState.drawerState.open() }
                    }
                )
            }

            // Statistics Screen
            composable(Screen.Statistics.route) {
                currentRoute = "statistics"
                val viewModel = remember {
                    TodoStatisticsViewModel(getStatisticsUseCase)
                }
                TodoStatisticsPage(
                    viewModel = viewModel,
                    onBackToDemo = {
                        parentNavController.popBackStack()
                    },
                    onOpenDrawer = {
                        scope.launch { scaffoldState.drawerState.open() }
                    }
                )
            }

            // Editor Screen (Add/Edit)
            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("todoId") { 
                    type = NavType.StringType
                    nullable = true 
                })
            ) { backStackEntry ->
                val todoId = backStackEntry.arguments?.getString("todoId")
                val viewModel = remember(todoId) {
                    TodoEditorViewModel(
                        todoId,
                        getTodoItemUseCase,
                        saveTodoUseCase
                    )
                }
                TodoEditorPage(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Detail Screen
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("todoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val todoId = backStackEntry.arguments?.getString("todoId") ?: return@composable
                val viewModel = remember(todoId) {
                    TodoDetailViewModel(
                        todoId,
                        getTodoItemUseCase,
                        deleteTodoUseCase,
                        toggleTodoStatusUseCase
                    )
                }
                TodoDetailPage(
                    viewModel = viewModel,
                    onEdit = { id ->
                        navController.navigate(Screen.Editor.createRoute(id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    OnBack {
        if (navController.currentBackStackEntry?.destination?.route == Screen.Home.route) {
            parentNavController.popBackStack()
        } else {
            navController.popBackStack()
        }
    }
}


@Composable
expect fun OnBack(onBack: () -> Unit)
