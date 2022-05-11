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

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*

/**
 * Handles all operations on Catbox as a signed-in user, using the given [userHash].
 *
 * For anonymous operations, see the public functions defined on the companion object.
 */
public class Catbox(internal val userHash: String) {
    public companion object {
        private val client: HttpClient
            get() = HttpClient {
                defaultRequest {
                    url("https://catbox.moe/user/api.php")
                }
            }

        private enum class ReqType(val value: String) {
            URL_UPLOAD("urlupload"),
            DELETE_FILES("deletefiles"),
            CREATE_ALBUM("createalbum"),
            EDIT_ALBUM("editalbum"),
            ADD_TO_ALBUM("addtoalbum"),
            REMOVE_FROM_ALBUM("removefromalbum"),
            DELETE_ALBUM("deletealbum"),
        }

        private suspend fun isCatboxError(response: HttpResponse, message: String): Boolean =
            response.status == HttpStatusCode.PreconditionFailed && response.bodyAsText() == message

        private suspend inline fun <reified T> request(
            reqType: ReqType,
            userHash: String?,
            parameterBuilder: ParametersBuilder.() -> Unit,
        ): T = client.use {
            it.submitForm(formParameters = Parameters.build {
                append("reqtype", reqType.value)
                if (userHash != null) append("userhash", userHash)
                parameterBuilder()
            }).body()
        }

        // TODO: calculate size of bytearray before uploading to ensure we don't exceed max size?
        internal suspend fun uploadImpl(
            content: ByteArray,
            name: String,
            userHash: String?,
        ): String {
            require(name.isNotBlank()) { "'name' must not be blank" }
            return client.use {
                it.submitFormWithBinaryData(
                    formData = formData {
                        append("reqtype", "fileupload")
                        if (userHash != null) append("userhash", userHash)
                        append("fileToUpload", content, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=$name")
                        })
                    },
                )
            }.body()
        }

        internal suspend fun uploadImpl(
            url: Url,
            userHash: String?,
        ): String = request(ReqType.URL_UPLOAD, userHash) { append("url", url.toString()) }

        /**
         * Uploads the given [content] to Catbox anonymously and returns the url pointing to the uploaded file.
         *
         * @param content the contents to upload
         * @param name the name of the file, must not be blank
         *
         * @throws IllegalArgumentException if [name] is blank
         */
        public suspend fun upload(content: ByteArray, name: String): String = uploadImpl(content, name, userHash = null)

        /**
         * Uploads the given [url] to Catbox anonymously and returns the url pointing to the uploaded file.
         *
         * @param url the url to upload
         */
        public suspend fun upload(url: Url): String = uploadImpl(url, userHash = null)

        /**
         * Creates a new Catbox album anonymously with the given [title] and [description], containing the given
         * [files], and returns the url to it.
         *
         * As this album is anonymous, it can *not* be edited after its creation.
         *
         * Note that unlike [delete], this function will *not* thrown an exception if `files` contains an invalid file
         * name, and will instead just create a "corrupt" entry on the album.
         *
         * @param title the title of the album
         * @param description the description of the album
         * @param files a sequence of file names to include on the album
         */
        public suspend fun createAlbum(
            title: String,
            description: String,
            files: Set<String>,
        ): String = request(ReqType.CREATE_ALBUM, userHash = null) {
            append("title", title)
            append("desc", description)
            append("files", files.toCatboxFiles())
        }
    }

    /**
     * Uploads the given [content] to Catbox and returns the url pointing to the uploaded file.
     *
     * @param content the contents to upload
     * @param name the name of the file, must not be empty
     *
     * @throws IllegalArgumentException if [name] is blank
     */
    public suspend fun upload(content: ByteArray, name: String): String = uploadImpl(content, name, userHash)

    /**
     * Uploads the given [url] to Catbox and returns the url pointing to the uploaded file.
     *
     * @param url the url to upload
     */
    public suspend fun upload(url: Url): String = uploadImpl(url, userHash)

    /**
     * Deletes the given [files] from Catbox, if any of the given files do not exist a [NoSuchCatboxFileException]
     * will be thrown.
     *
     * Note that Catbox *will* delete all files in the given sequence until it encounters an invalid file. For example,
     * if given the set `[a.png, b.png, c.png]`, wherein `a.png` and `c.png` are valid files, then only `a.png` will
     * be deleted, as `b.png` is invalid and does not exist. If `a.png` and `b.png` are valid, but `c.png` is not, then
     * both `a.png` and `b.png` will be deleted, etc.
     *
     * @param files a sequence of file names to delete, must be valid Catbox files, any spaces in the entries will be
     * removed
     *
     * @throws NoSuchCatboxFileException if [files] contains one or more invalid Catbox file names
     */
    public suspend fun delete(files: Set<String>) {
        try {
            request<Unit>(ReqType.DELETE_FILES, userHash) {
                append("files", files.toCatboxFiles())
            }
        } catch (e: ClientRequestException) {
            if (isCatboxError(e.response, NO_SUCH_FILE_ERROR)) {
                throw NoSuchCatboxFileException(e)
            } else {
                throw e
            }
        }
    }

    /**
     * Creates a new Catbox album with the given [title] and [description], containing the given [files], and returns
     * the url to it.
     *
     * Note that unlike [delete], this function will *not* thrown an exception if `files` contains an invalid file name,
     * and will instead just create a "corrupt" entry on the album.
     *
     * @param title the title of the album
     * @param description the description of the album
     * @param files a sequence of file names to include on the album, must not contain more than 500 entries
     *
     * @throws IllegalArgumentException if [files] contains more than 500 entries
     */
    public suspend fun createAlbum(
        title: String,
        description: String,
        files: Set<String>,
    ): String {
        require(files.size <= 500) { "Albums can only contain 500 files, was given ${files.size}." }
        return request(ReqType.CREATE_ALBUM, userHash) {
            append("title", title)
            append("desc", description)
            append("files", files.toCatboxFiles())
        }
    }

    /**
     * Edits the album with the given [short], changing the title to the given [title], description to the given
     * [description] and all files to the given [files].
     *
     * Note that unlike [delete], this function will *not* thrown an exception if `files` contains an invalid file name,
     * and will instead just create a "corrupt" entry on the album.
     *
     * This function will directly edit the given album, meaning that there is no appending of files, all given
     * parameters will *directly replace* the already existing ones.
     *
     * @param short the identifier of the album
     * @param title the new title
     * @param description the new description
     * @param files the new sequence of file names to include on the album, must not contain more than 500 entries
     *
     * @throws IllegalArgumentException if [files] contains more than 500 entries
     * @throws NoSuchCatboxAlbumException if [short] points to a non-existent album, or an album the current user can't
     * modify
     */
    public suspend fun editAlbum(
        short: String,
        title: String,
        description: String,
        files: Set<String>,
    ) {
        require(files.size <= 500) { "Albums can only contain 500 files, was given ${files.size}." }
        requestAlbum<Unit>(short, ReqType.EDIT_ALBUM) {
            append("title", title)
            append("desc", description)
            append("files", files.toCatboxFiles())
        }
    }

    /**
     * Adds the given [files] to the album pointed at by the given [short].
     *
     * @param short the identifier of the album to add the given [files] to
     * @param files a sequence of file names to add to the album
     *
     * @throws NoSuchCatboxAlbumException if [short] points to a non-existent album, or an album the current user can't
     * modify
     */
    // TODO: does this throw errors on invalid files?
    public suspend fun addToAlbum(
        short: String,
        files: Set<String>,
    ) {
        requestAlbum<Unit>(short, ReqType.ADD_TO_ALBUM) {
            append("files", files.toCatboxFiles())
        }
    }

    /**
     * Removes the given [files] from the album pointed at by the given [short].
     *
     * @param short the identifier of the album from which to remove the given [files]
     * @param files a sequence of file names to remove from the album
     *
     * @throws NoSuchCatboxAlbumException if [short] points to a non-existent album, or an album the current user can't
     * modify
     */
    // TODO: does this throw errors on invalid files?
    public suspend fun removeFromAlbum(
        short: String,
        files: Set<String>,
    ) {
        requestAlbum<Unit>(short, ReqType.REMOVE_FROM_ALBUM) {
            append("files", files.toCatboxFiles())
        }
    }

    /**
     * Deletes the album pointed at by the given [short].
     *
     * @param short the identifier of the album to delete
     *
     * @throws NoSuchCatboxAlbumException if [short] points to a non-existent album, or an album the current user can't
     * modify
     */
    public suspend fun deleteAlbum(short: String) {
        requestAlbum<Unit>(short, ReqType.DELETE_ALBUM)
    }

    private suspend inline fun <reified T> requestAlbum(
        short: String,
        reqType: ReqType,
        parameterBuilder: ParametersBuilder.() -> Unit = {},
    ): T = try {
        request(reqType, userHash) {
            append("short", short)
            parameterBuilder()
        }
    } catch (e: ClientRequestException) {
        when {
            isCatboxError(e.response, NO_SUCH_ALBUM_ERROR) -> throw NoSuchCatboxAlbumException(short, e)
            isCatboxError(e.response, NO_SUCH_FILE_ERROR) -> throw NoSuchCatboxFileException(e)
            else -> throw e
        }
    }
}