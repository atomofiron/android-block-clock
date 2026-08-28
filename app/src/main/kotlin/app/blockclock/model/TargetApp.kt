package app.blockclock.model

/** An app to launch on a widget tap: package + launcher activity. */
data class TargetApp(
    val packageName: String,
    val activityName: String,
)