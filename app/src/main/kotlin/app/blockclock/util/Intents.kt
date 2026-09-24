package app.blockclock.util

import android.content.Intent
import androidx.core.net.toUri

private const val GITHUB_URL = "https://github.com/atomofiron/android-block-clock"

object Intents {

    val repository = Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())

    fun license(url: String) = Intent(Intent.ACTION_VIEW, url.toUri())
}
