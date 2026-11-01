package com.auralis.app.data.download

import android.net.Uri

/** Storage operations used by LibraryRepository; identity is always explicit. */
interface DownloadStore {
    fun hasLocalCopy(identity: DownloadIdentity): Boolean
    fun uriForIdentity(identity: DownloadIdentity): Uri?
    fun cancel(identity: DownloadIdentity)
    fun download(
        identity: DownloadIdentity,
        url: String,
        onProgress: ((Float) -> Unit)? = null,
    ): Uri
    fun deleteExact(identity: DownloadIdentity): Boolean
}
