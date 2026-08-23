package app.atomofiron.blockclock.util

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import app.blockclock.R
import app.atomofiron.blockclock.update.model.ApkInfo

fun PackageManager.apkInfo(path: String, icon: Boolean = true, signature: Boolean = false): ApkInfo? {
    val packageInfo = getPackageArchiveInfo(path, 0)
    val info = packageInfo?.applicationInfo
    info ?: return null
    info.sourceDir = path
    info.publicSourceDir = path
    return ApkInfo(
        appName = info.loadLabel(this).toString(),
        versionName = packageInfo.versionName.toString(),
        versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toInt(),
    )
}

fun PackageManager.launchable(packageName: String): Boolean = getLaunchIntentForPackage(packageName) != null

fun Context.launch(packageName: String): Boolean {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    launchIntent ?: return false.also {
        Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show()
    }
    startActivity(launchIntent)
    return true
}
