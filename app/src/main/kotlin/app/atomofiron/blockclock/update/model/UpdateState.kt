package app.atomofiron.blockclock.update.model

sealed class UpdateState(val interactable: Boolean = false) {
    data object Unknown : UpdateState(interactable = true)
    data class Error(val message: String?) : UpdateState(interactable = true)
    data object UpToDate : UpdateState()
    data object Checking : UpdateState()
    data class Available(val type: UpdateType, val code: Int) : UpdateState(interactable = true)
    data class Downloading(val progress: Float?) : UpdateState()
    data object Completable : UpdateState(interactable = true)
    data object Installing : UpdateState()

    fun progress(): Float? = when (this) {
        is Downloading -> progress
        else -> null
    }

    fun processing(): Boolean = when (this) {
        is Installing,
        is Checking,
        is Downloading -> true
        else -> false
    }
}