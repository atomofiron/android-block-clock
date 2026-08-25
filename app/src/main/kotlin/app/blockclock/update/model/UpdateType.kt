package app.blockclock.update.model

sealed interface UpdateType {
    sealed interface Variant : UpdateType

    data object Immediate : Variant
    data object Flexible : Variant
    data object All : UpdateType
}