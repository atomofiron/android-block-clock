package app.atomofiron.blockclock.model

import kotlinx.serialization.Serializable

@Serializable
data class GithubAsset(
    val id: Int,
    val name: String,
    val size: Long,
    val browserDownloadUrl: String,
)
