package com.emomtimer.data.update

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import com.emomtimer.domain.model.AppUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadStatus {
    data class InProgress(val percent: Int) : DownloadStatus
    data class Successful(val file: File) : DownloadStatus
    data class Failed(val reason: String) : DownloadStatus
}

@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueue(update: AppUpdate): Long {
        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setTitle("DK Timer ${update.tagName}")
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "dk-timer-${update.tagName}.apk",
            )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType(APK_MIME_TYPE)
        return downloadManager.enqueue(request)
    }

    fun queryStatus(downloadId: Long): DownloadStatus {
        downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return DownloadStatus.Failed("Download not found")

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> successfulStatus(cursor)
                DownloadManager.STATUS_FAILED -> failedStatus(cursor)
                else -> inProgressStatus(cursor)
            }
        }
    }

    private fun successfulStatus(cursor: Cursor): DownloadStatus {
        val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
        val path = Uri.parse(localUri).path ?: return DownloadStatus.Failed("Missing local file path")
        return DownloadStatus.Successful(File(path))
    }

    private fun failedStatus(cursor: Cursor): DownloadStatus {
        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
        return DownloadStatus.Failed("Download failed (reason $reason)")
    }

    private fun inProgressStatus(cursor: Cursor): DownloadStatus {
        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        val percent = if (total > 0) ((downloaded * PERCENT_MAX) / total).toInt() else 0
        return DownloadStatus.InProgress(percent)
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val PERCENT_MAX = 100
    }
}
