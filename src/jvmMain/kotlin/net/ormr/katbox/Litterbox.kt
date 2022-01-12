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

/**
 * Uploads the given [file] to Litterbox and returns the url pointing to the uploaded file.
 *
 * @param file the contents to upload, must not be larger than 1 GiB
 * @param name the name of the file, must not be blank
 * @param time how long the file should be available for
 *
 * @throws IllegalArgumentException if [file] or [name] is faulty in some manner
 */
private suspend fun Litterbox.upload(
    file: Path,
    name: String = file.name,
    time: LitterboxTime = LitterboxTime.HOUR_1,
): String {
    require(file.exists()) { "'file' needs to exist" }
    require(file.isRegularFile()) { "'file' must be a regular file" }
    require(file.fileSize() <= MAX_LITTERBOX_FILE_SIZE) { "'file' can't be larger than 1 GiB" }
    return upload(file.readBytes(), name, time)
}