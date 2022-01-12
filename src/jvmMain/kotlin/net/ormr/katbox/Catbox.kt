/*
 * Copyright 2022 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("BlockingMethodInNonBlockingContext")

package net.ormr.katbox

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readBytes

private suspend fun uploadImpl(file: Path, name: String, userHash: String?): String {
    require(file.exists()) { "'file' needs to exist" }
    require(file.isRegularFile()) { "'file' must be a regular file" }
    require(file.fileSize() <= MAX_CATBOX_FILE_SIZE) { "'file' can't be larger than 200 MiB" }
    return Catbox.uploadImpl(file.readBytes(), name, userHash)
}

/**
 * Uploads the given [file] to Catbox anonymously and returns the url pointing to the uploaded file.
 *
 * @param file the file to upload, must not be larger than 200 MiB
 * @param name the name of the file, must not be empty
 *
 * @throws IllegalArgumentException if [file] or [name] is faulty in some manner
 */
public suspend fun Catbox.Companion.upload(file: Path, name: String = file.name): String =
    uploadImpl(file, name, userHash = null)

/**
 * Uploads the given [file] to Catbox and returns the url pointing to the uploaded file.
 *
 * @param file the file to upload, must not be larger than 200 MiB
 * @param name the name of the file, must not be empty
 *
 * @throws IllegalArgumentException if [file] or [name] is faulty in some manner
 */
public suspend fun Catbox.upload(file: Path, name: String = file.name): String = uploadImpl(file, name, userHash)