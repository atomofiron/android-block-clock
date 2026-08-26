package app.blockclock.ui.insets

data class ScreenCorners(
    val topLeft: Int,
    val topRight: Int,
    val bottomRight: Int,
    val bottomLeft: Int,
) {
    companion object {
        val Zero = ScreenCorners(0, 0, 0, 0)
    }
}