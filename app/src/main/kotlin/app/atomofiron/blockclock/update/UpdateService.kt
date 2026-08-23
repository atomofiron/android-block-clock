package app.atomofiron.blockclock.update

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import app.atomofiron.blockclock.update.model.UpdateType
import app.atomofiron.blockclock.util.AppScope

interface UpdateService {
    companion object {
        const val ACTION_INSTALL_UPDATE = "ACTION_INSTALL_UPDATE"

        lateinit var self: UpdateService
            private set

        fun init(
            context: Context,
            scope: AppScope,
            updateStore: UpdateStore,
            factory: Factory,
        ) {
            self = factory.new(
                context = context,
                scope = scope,
                updateStore = updateStore,
            )
        }
    }

    fun onActivityCreate(activity: AppCompatActivity)
    fun check(userAction: Boolean = false)
    fun retry()
    fun startUpdate(variant: UpdateType.Variant)
    fun completeUpdate()

    interface Factory {
        fun new(
            context: Context,
            scope: AppScope,
            updateStore: UpdateStore,
        ): UpdateService
    }
}
