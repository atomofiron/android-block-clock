package app.atomofiron.blockclock

import app.atomofiron.blockclock.update.AppSource

class App : AbstractApp() {
    override val appSource = AppSource.GitHub
    override val updateServiceFactory = AppUpdateServiceGithubImpl.Companion
}