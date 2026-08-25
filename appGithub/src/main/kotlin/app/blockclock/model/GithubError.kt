package app.blockclock.model

import kotlinx.serialization.Serializable

@Serializable
class GithubError(
    val status: String? = null,
    val message: String,
)
