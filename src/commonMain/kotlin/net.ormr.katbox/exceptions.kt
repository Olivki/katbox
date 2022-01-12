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

/**
 * Thrown when Catbox has been given file names that do not exist on its server.
 */
public class NoSuchCatboxFileException(cause: Throwable) : RuntimeException("Catbox was given an invalid file.", cause)

/**
 * Thrown when an invalid Catbox album short has been given to Catbox.
 */
public class NoSuchCatboxAlbumException(short: String, cause: Throwable) :
    RuntimeException("Album '$short' either does not exist, or does not belong to the current user.", cause)