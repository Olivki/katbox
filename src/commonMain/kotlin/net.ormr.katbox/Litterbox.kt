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

import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.url
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/**
 * Handles all operations on Litterbox.
 */
public object Litterbox {
    private val client = HttpClient {
        defaultRequest {
            url("https://litterbox.catbox.moe/resources/internals/api.php")
        }
    }

    /**
     * Uploads the given [content] to Litterbox and returns the url pointing to the uploaded file.
     *
     * @param content the contents to upload
     * @param name the name of the file, must not be blank
     * @param time how long the file should be available for
     *
     * @throws IllegalArgumentException if [name] is blank
     */
    // TODO: calculate size of bytearray before uploading to ensure we don't exceed max size?
    public suspend fun upload(
        content: ByteArray,
        name: String,
        time: LitterboxTime = LitterboxTime.HOUR_1,
    ): String {
        require(name.isNotBlank()) { "'name' must not be blank" }
        return client.submitFormWithBinaryData(
            formData = formData {
                append("reqtype", "fileupload")
                append("time", time.value)
                append("fileToUpload", content, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=$name")
                })
            }
        )
    }
}

