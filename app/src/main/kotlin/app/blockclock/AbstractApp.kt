package app.blockclock

import android.annotation.SuppressLint
import android.app.Application
import app.blockclock.update.AppSource
import app.blockclock.update.UpdateService
import app.blockclock.update.UpdateStore
import app.blockclock.util.AppScope

abstract class AbstractApp : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var self: AbstractApp // this is the Application instance
            private set
    }

    protected abstract val appSource: AppSource
    protected abstract val updateServiceFactory: UpdateService.Factory

    override fun onCreate() {
        super.onCreate()

        self = this

        val scope = AppScope()
        UpdateStore.init(this, appSource, scope)
        UpdateService.init(this, scope, UpdateStore.self, updateServiceFactory)

        if (!BuildConfig.DEBUG && appSource == AppSource.GooglePlay) {
            UpdateService.self.check()
        }
    }
}
