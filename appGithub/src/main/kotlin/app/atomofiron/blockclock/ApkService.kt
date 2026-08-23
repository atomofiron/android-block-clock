package app.atomofiron.blockclock

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import app.atomofiron.blockclock.util.Android
import app.atomofiron.blockclock.util.Rslt
import app.atomofiron.blockclock.util.launch
import app.atomofiron.blockclock.util.launchable
import app.atomofiron.blockclock.util.toRslt
import java.io.FileInputStream

class ApkService(
    private val context: Context,
    private val installer: PackageInstaller,
) {
    fun installApk(path: String, action: String, stringId: String? = null, silently: Boolean = false): Rslt<Unit> {
        val stream = FileInputStream(path)
        val length = stream.available().toLong()
        return install(length, action, silently) {
            openWrite(stringId ?: "unused", 0, length).use { output ->
                stream.use {
                    it.copyTo(output)
                    fsync(output)
                }
            }
            return@install Rslt.Ok
        }
    }

    private fun install(
        length: Long,
        action: String,
        silently: Boolean = false,
        block: PackageInstaller.Session.() -> Rslt<Unit>,
    ): Rslt<Unit> = try {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (silently) {
            params.setAppPackageName(context.packageName)
            params.setSize(length)
            if (Android.S) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            if (Android.T) params.setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.block()
            val intent = Intent(context, UpdateInstallReceiver::class.java)
            intent.action = action
            intent.setPackage(context.packageName)
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            try {
                session.commit(pendingIntent.intentSender)
                Rslt.Ok
            } catch (e: Exception) {
                e.toRslt()
            }
        }
    } catch (e: Exception) {
        e.toRslt()
    }

    fun launchable(packageName: String): Boolean = context.packageManager.launchable(packageName)

    fun launchApk(packageName: String) = context.launch(packageName)
}