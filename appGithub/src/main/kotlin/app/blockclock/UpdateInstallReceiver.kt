package app.blockclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.EXTRA_STATUS
import android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE
import android.content.pm.PackageInstaller.STATUS_FAILURE
import android.content.pm.PackageInstaller.STATUS_FAILURE_ABORTED
import android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION
import android.content.pm.PackageInstaller.STATUS_SUCCESS
import androidx.core.content.IntentCompat
import app.blockclock.update.UpdateStore
import app.blockclock.util.Alert

class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(EXTRA_STATUS, STATUS_FAILURE)) {
            STATUS_SUCCESS -> return
            STATUS_FAILURE_ABORTED -> Unit
            STATUS_PENDING_USER_ACTION -> {
                val activityIntent = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                context.startActivity(activityIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            else -> intent.getStringExtra(EXTRA_STATUS_MESSAGE)
                ?.let { UpdateStore.self.showUpdateAlert(Alert(it)) }
        }
        UpdateStore.self.fallback()
    }
}