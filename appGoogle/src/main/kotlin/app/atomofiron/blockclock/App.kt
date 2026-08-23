package app.atomofiron.blockclock

import app.atomofiron.blockclock.update.AppSource

class App : AbstractApp() {
    override val appSource = AppSource.GooglePlay
    override val updateServiceFactory = AppUpdateServiceGoogleImpl.Companion
}