package app.blockclock

import app.blockclock.update.AppSource

class App : AbstractApp() {
    override val appSource = AppSource.GooglePlay
    override val updateServiceFactory = UpdateServiceGoogleImpl.Companion
}