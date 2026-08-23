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

package com.bytekmp.sample.todo.data

import com.bytedance.kmp.spi.kmpService
import com.bytekmp.sample.todo.domain.ITodoRepository
import com.bytekmp.sample.todo.domain.TodoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data store implementation for Todo items.
 * Uses local file storage for persistence.
 */
class TodoDataStore : ITodoRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val fileStorage = kmpService<ITodoFileStorage>() ?: error("ITodoFileStorage not implemented")

    private val _itemsFlow = MutableStateFlow<List<TodoItem>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            loadFromDisk()
        }
    }

    override fun observeAll(): Flow<List<TodoItem>> = _itemsFlow.asStateFlow()

    override suspend fun getAll(): List<TodoItem> = _itemsFlow.value

    override suspend fun getById(id: String): TodoItem? {
        return _itemsFlow.value.find { it.id == id }
    }

    override suspend fun addOrUpdate(item: TodoItem) {
        val currentList = _itemsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index != -1) {
            currentList[index] = item
        } else {
            currentList.add(item)
        }
        updateStateAndSave(currentList)
    }

    override suspend fun delete(id: String) {
        val currentList = _itemsFlow.value.filter { it.id != id }
        updateStateAndSave(currentList)
    }

    override suspend fun clearCompleted() {
        val currentList = _itemsFlow.value.filter { !it.isDone }
        updateStateAndSave(currentList)
    }

    private suspend fun updateStateAndSave(newList: List<TodoItem>) {
        _itemsFlow.value = newList
        saveToDisk(newList)
    }

    private suspend fun loadFromDisk() {
        try {
            val content = fileStorage.getContent()
            if (!content.isNullOrBlank()) {
                val items = json.decodeFromString<List<TodoItem>>(content)
                _itemsFlow.value = items
            }
        } catch (e: Exception) {
            _itemsFlow.value = emptyList()
        }
    }

    private suspend fun saveToDisk(items: List<TodoItem>) {
        try {
            val content = json.encodeToString(items)
            fileStorage.saveContent(content)
        } catch (e: Exception) {
        }
    }
}
