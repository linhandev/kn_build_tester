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

@file:OptIn(ExperimentalForeignApi::class)

package com.bytekmp.sample.todo.data

import com.bytedance.kmp.spi.annotation.KmpSpiImpl
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

@KmpSpiImpl(ITodoFileStorage::class)
class TodoFileStorageImpl: ITodoFileStorage {
    private val filePath = "${getCacheDir()}/bytekmp_todo.json"

    private val fileManager = NSFileManager.defaultManager

    private fun getCacheDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            directory = NSCachesDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        )
        return paths.first() as String
    }

    override suspend fun getContent(): String? {
        return NSString.stringWithContentsOfFile(filePath, encoding = NSUTF8StringEncoding, error = null) ?: ""
    }

    override suspend fun saveContent(content: String) {
        createIfNeed()
        val nsContent = content as NSString // 将 Kotlin String 转换为 NSString
        nsContent.writeToFile(filePath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    private fun createIfNeed() {
        if (!fileManager.fileExistsAtPath(filePath)) {
            fileManager.createFileAtPath(filePath, contents = null, attributes = null)
        }
    }
}