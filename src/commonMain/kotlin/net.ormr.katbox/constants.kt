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

package net.ormr.katbox

private const val KiB = 1024
private const val MiB = KiB * KiB
private const val GiB = MiB * KiB
internal const val MAX_CATBOX_FILE_SIZE = 200 * MiB
internal const val MAX_LITTERBOX_FILE_SIZE = 1 * GiB
internal const val NO_SUCH_FILE_ERROR = "File doesn't exist?"
internal const val NO_SUCH_ALBUM_ERROR = "No album found for user specified."