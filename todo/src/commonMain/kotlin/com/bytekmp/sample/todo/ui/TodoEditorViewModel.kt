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
import com.bytekmp.sample.todo.domain.usecase.GetTodoItemUseCase
import com.bytekmp.sample.todo.domain.usecase.SaveTodoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

class TodoEditorViewModel(
    private val todoId: String?,
    private val getTodoItemUseCase: GetTodoItemUseCase,
    private val saveTodoUseCase: SaveTodoUseCase
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    init {
        if (todoId != null) {
            viewModelScope.launch {
                _isLoading.value = true
                val item = getTodoItemUseCase(todoId)
                item?.let {
                    _title.value = it.title
                    _description.value = it.description
                }
                _isLoading.value = false
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _title.value = newTitle
    }

    fun onDescriptionChanged(newDesc: String) {
        _description.value = newDesc
    }

    fun saveTodo() {
        // Validate title is not empty
        if (_title.value.trim().isEmpty()) {
            _validationError.value = "Title cannot be empty"
            return
        }
        
        _validationError.value = null // Clear previous error

        viewModelScope.launch {
            _isLoading.value = true
            val newItem = TodoItem(
                id = todoId ?: Random.nextLong().toString(), // Simple ID generation
                title = _title.value.trim(),
                description = _description.value.trim(),
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            saveTodoUseCase(newItem)
            _isLoading.value = false
            _isSaved.value = true
        }
    }

    fun clearValidationError() {
        _validationError.value = null
    }
}
