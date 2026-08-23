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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytekmp.sample.todo.domain.TodoItem
import com.bytekmp.sample.todo.domain.usecase.GetTodosUseCase
import com.bytekmp.sample.todo.domain.usecase.ToggleTodoStatusUseCase
import com.bytekmp.sample.todo.domain.usecase.ClearCompletedUseCase
import com.bytekmp.sample.todo.domain.usecase.TodoFilterType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TodoHomeUiState(
    val items: List<TodoItem> = emptyList(),
    val filter: TodoFilterType = TodoFilterType.ALL,
    val isLoading: Boolean = false
)

class TodoHomeViewModel(
    private val getTodosUseCase: GetTodosUseCase,
    private val toggleTodoStatusUseCase: ToggleTodoStatusUseCase,
    private val clearCompletedUseCase: ClearCompletedUseCase,
    private val deleteTodoUseCase: com.bytekmp.sample.todo.domain.usecase.DeleteTodoUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(TodoFilterType.ALL)
    private val _isLoading = MutableStateFlow(false)

    // Combined stream to trigger use case re-execution or filtering
    val uiState: StateFlow<TodoHomeUiState> = _filter
        .flatMapLatest { filterType ->
            getTodosUseCase(filterType).map { items ->
                TodoHomeUiState(
                    items = items,
                    filter = filterType,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TodoHomeUiState(isLoading = true)
        )

    fun setFilter(filter: TodoFilterType) {
        _filter.value = filter
    }

    fun toggleTodo(item: TodoItem) {
        viewModelScope.launch {
            toggleTodoStatusUseCase(item)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            clearCompletedUseCase()
        }
    }

    fun deleteTodo(id: String) {
        viewModelScope.launch {
            deleteTodoUseCase(id)
        }
    }
}
