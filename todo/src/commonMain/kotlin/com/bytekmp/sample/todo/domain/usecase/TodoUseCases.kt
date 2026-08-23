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

package com.bytekmp.sample.todo.domain.usecase

import com.bytekmp.sample.todo.domain.ITodoRepository
import com.bytekmp.sample.todo.domain.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetTodosUseCase(private val repository: ITodoRepository) {
    operator fun invoke(filterType: TodoFilterType): Flow<List<TodoItem>> {
        return repository.observeAll().map { items ->
            when (filterType) {
                TodoFilterType.ALL -> items
                TodoFilterType.ACTIVE -> items.filter { !it.isDone }
                TodoFilterType.COMPLETED -> items.filter { it.isDone }
            }
        }
    }
}

class ToggleTodoStatusUseCase(private val repository: ITodoRepository) {
    suspend operator fun invoke(item: TodoItem) {
        // Toggle the status
        repository.addOrUpdate(item.copy(isDone = !item.isDone))
    }
}

class SaveTodoUseCase(private val repository: ITodoRepository) {
    suspend operator fun invoke(item: TodoItem) {
        repository.addOrUpdate(item)
    }
}

class DeleteTodoUseCase(private val repository: ITodoRepository) {
    suspend operator fun invoke(id: String) {
        repository.delete(id)
    }
}

class ClearCompletedUseCase(private val repository: ITodoRepository) {
    suspend operator fun invoke() {
        repository.clearCompleted()
    }
}


class GetTodoItemUseCase(private val repository: ITodoRepository) {
    suspend operator fun invoke(id: String): TodoItem? {
        return repository.getById(id)
    }
}

enum class TodoFilterType {
    ALL, ACTIVE, COMPLETED
}
