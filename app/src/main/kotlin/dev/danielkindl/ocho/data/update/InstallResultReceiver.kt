package dev.danielkindl.ocho.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast

class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> forwardUserActionIntent(context, intent)
            PackageInstaller.STATUS_SUCCESS -> showToast(context, "DK Timer updated")
            else -> showToast(context, installFailureMessage(intent, status))
        }
    }

    private fun forwardUserActionIntent(context: Context, intent: Intent) {
        intent.confirmationIntent()
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { context.startActivity(it) }
    }

    private fun installFailureMessage(intent: Intent, status: Int): String {
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        return "Update failed: ${message ?: "status $status"}"
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    @Suppress("DEPRECATION")
    private fun Intent.confirmationIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_INTENT)
        }
}
