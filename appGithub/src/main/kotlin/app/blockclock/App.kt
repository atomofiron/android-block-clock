package app.blockclock

import app.blockclock.update.AppSource

class App : AbstractApp() {
    override val appSource = AppSource.GitHub
    override val updateServiceFactory = UpdateServiceGithubImpl.Companion
}