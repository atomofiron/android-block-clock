package app.blockclock.util

import android.util.Log

fun Any.logd(s: String) {
    app.blockclock.util.logd("[${this.javaClass.simpleName}] $s")
}

fun logd(s: String) {
    Log.e("blockclock", s)
}