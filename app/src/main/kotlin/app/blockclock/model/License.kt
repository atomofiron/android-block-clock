package app.blockclock.model

sealed interface License {
    val name: String

    data class Text(
        override val name: String,
        val text: String,
    ) : License

    data class Url(
        override val name: String,
        val url: String,
    ) : License
}