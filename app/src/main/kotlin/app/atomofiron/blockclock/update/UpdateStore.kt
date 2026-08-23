package app.atomofiron.blockclock.update

import android.content.Context
import androidx.core.content.edit
import app.atomofiron.blockclock.update.model.UpdateState
import app.atomofiron.blockclock.util.Alert
import app.atomofiron.blockclock.util.AppScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UpdateStore(
    context: Context,
    val source: AppSource,
    val scope: AppScope,
) {
    companion object {
        const val KEY_UPDATE_CODE = "app_update_code"

        lateinit var self: UpdateStore
            private set

        fun init(context: Context, source: AppSource, scope: AppScope) {
            self = UpdateStore(context, source, scope)
        }
    }

    private var fallback: UpdateState = UpdateState.Unknown
    val state: StateFlow<UpdateState>
        field = MutableStateFlow<UpdateState>(UpdateState.Unknown)

    val alerts: SharedFlow<Alert.Uni>
        field = MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)

    private val appUpdateCodeFlow = MutableStateFlow(prefs.getInt(KEY_UPDATE_CODE, 0))
    val appUpdateCode: StateFlow<Int> = appUpdateCodeFlow

    fun set(state: UpdateState) {
        when (state) {
            is UpdateState.Completable,
            is UpdateState.Available -> fallback = state
            else -> Unit
        }
        this.state.value = state
    }

    fun fallback() {
        state.value = fallback
    }

    operator fun invoke(block: UpdateStore.() -> Unit) = block()

    fun setAppUpdateCode(value: Int) {
        prefs.edit { putInt(KEY_UPDATE_CODE, value) }
        appUpdateCodeFlow.value = value
    }

    fun showUpdateAlert(message: Alert.Uni) = scope.launch {
        alerts.emit(message)
    }
}