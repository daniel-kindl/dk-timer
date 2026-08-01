package dev.danielkindl.ocho.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

/**
 * Installs a downloaded APK over the running app.
 *
 * Takes two routes by API level. From Android 12 the `PackageInstaller` session API
 * can install without a per-install confirmation dialog; below that the only option
 * is an `ACTION_VIEW` intent that hands off to the system installer UI.
 *
 * Either route needs the user to have granted "install unknown apps" first — see
 * [canInstallPackages] and [unknownSourcesSettingsIntent].
 */
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Whether the user has granted this app permission to install packages. */
    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Settings screen where the user grants that permission; launch when [canInstallPackages] is false. */
    fun unknownSourcesSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Starts installing [apkFile]. Returns as soon as the install is handed off;
     * the outcome arrives at [InstallResultReceiver], or via the system UI on
     * older versions.
     */
    fun install(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installViaPackageInstaller(apkFile)
        } else {
            installViaIntent(apkFile)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun installViaPackageInstaller(apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = packageInstaller.createSession(params)
        packageInstaller.openSession(sessionId).use { session ->
            FileInputStream(apkFile).use { input ->
                session.openWrite(SESSION_NAME, 0L, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            session.commit(installResultPendingIntent(sessionId).intentSender)
        }
    }

    private fun installViaIntent(apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun installResultPendingIntent(sessionId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            sessionId,
            Intent(context, InstallResultReceiver::class.java),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val SESSION_NAME = "package"
    }
}
