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
import com.bytekmp.sample.todo.domain.usecase.DeleteTodoUseCase
import com.bytekmp.sample.todo.domain.usecase.GetTodoItemUseCase
import com.bytekmp.sample.todo.domain.usecase.ToggleTodoStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodoDetailViewModel(
    private val todoId: String,
    private val getTodoItemUseCase: GetTodoItemUseCase,
    private val deleteTodoUseCase: DeleteTodoUseCase,
    private val toggleTodoStatusUseCase: ToggleTodoStatusUseCase
) : ViewModel() {

    private val _item = MutableStateFlow<TodoItem?>(null)
    val item = _item.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted = _isDeleted.asStateFlow()

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            _item.value = getTodoItemUseCase(todoId)
        }
    }

    fun deleteTodo() {
        viewModelScope.launch {
            deleteTodoUseCase(todoId)
            _isDeleted.value = true
        }
    }

    fun toggleStatus() {
        val currentItem = _item.value ?: return
        viewModelScope.launch {
            toggleTodoStatusUseCase(currentItem)
            loadItem() // Reload to reflect changes
        }
    }
}
