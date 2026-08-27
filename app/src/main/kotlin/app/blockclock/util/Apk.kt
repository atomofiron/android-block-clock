package app.blockclock.util

import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import app.blockclock.update.model.ApkInfo

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
