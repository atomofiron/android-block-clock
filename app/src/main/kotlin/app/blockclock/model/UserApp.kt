package app.blockclock.model

import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

@ConsistentCopyVisibility
data class UserApp private constructor(
    val label: String,
    val packageName: String,
    val activityName: String,
    val drawable: Drawable?,
) {
    val key: String = "$packageName/$activityName"

    constructor(
        label: String,
        info: ResolveInfo,
        drawable: Drawable?,
    ) : this(
        label = label,
        packageName = info.activityInfo.packageName,
        activityName = info.activityInfo.name,
        drawable,
    )

    fun toTarget() = TargetApp(packageName, activityName)
}