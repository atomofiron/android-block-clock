package app.blockclock.model

enum class ColorSource {
    Manual,
    Primary,
    Secondary,
    Tertiary,
    ;
    companion object {
        fun from(name: String?): ColorSource = entries
            .takeIf { name != null }
            ?.find { it.name == name }
            ?: Manual
    }
    fun manual() = this == Manual
}