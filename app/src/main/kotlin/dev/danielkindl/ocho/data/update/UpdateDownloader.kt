package dev.danielkindl.ocho.data.update

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import android.os.Environment
import dev.danielkindl.ocho.domain.model.AppUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Where an in-flight APK download has got to, as reported by [UpdateDownloader.queryStatus]. */
sealed interface DownloadStatus {

    /** Still downloading. @property percent completion from 0 to 100. */
    data class InProgress(val percent: Int) : DownloadStatus

    /** Finished. @property file the downloaded APK, ready to hand to the installer. */
    data class Successful(val file: File) : DownloadStatus

    /** Gave up. @property reason human-readable explanation, shown directly to the user. */
    data class Failed(val reason: String) : DownloadStatus
}

private const val PERCENT_MAX = 100

/** Pure, Android-free percent calculation, guarded against a zero-byte [totalBytes]. */
internal fun computeDownloadPercent(downloadedBytes: Long, totalBytes: Long): Int =
    if (totalBytes > 0) ((downloadedBytes * PERCENT_MAX) / totalBytes).toInt() else 0

/**
 * Downloads release APKs via the system [DownloadManager].
 *
 * Uses the platform downloader rather than fetching in-process so a download
 * survives the app being backgrounded or killed mid-transfer, and so the user gets
 * a system progress notification for free.
 *
 * Files land in the app's external files directory, which needs no storage
 * permission and is cleaned up when the app is uninstalled.
 */
@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /**
     * Starts downloading [update]'s APK.
     *
     * @return the DownloadManager id to pass to [queryStatus]. Returns as soon as the
     *   download is queued, not when it completes.
     */
    fun enqueue(update: AppUpdate): Long {
        val request = DownloadManager.Request(update.downloadUrl.toUri())
            .setTitle("Ocho ${update.tagName}")
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "ocho-${update.tagName}.apk",
            )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType(APK_MIME_TYPE)
        return downloadManager.enqueue(request)
    }

    /**
     * Reads the current state of the download with [downloadId].
     *
     * Every state that is neither success nor failure — pending, running, paused —
     * reports as [DownloadStatus.InProgress], so a stalled download keeps showing
     * progress rather than looking like an error.
     */
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
        val path = localUri.toUri().path ?: return DownloadStatus.Failed("Missing local file path")
        return DownloadStatus.Successful(File(path))
    }

    private fun failedStatus(cursor: Cursor): DownloadStatus {
        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
        return DownloadStatus.Failed("Download failed (reason $reason)")
    }

    private fun inProgressStatus(cursor: Cursor): DownloadStatus {
        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        return DownloadStatus.InProgress(computeDownloadPercent(downloaded, total))
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
