package app.blockclock.licenses

import android.content.res.AssetManager
import java.nio.charset.StandardCharsets.UTF_8

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

/**
 * Reads OSS licenses from assets/licenses — the `third_party_license_metadata`
 * and `third_party_licenses` pair (the oss-licenses plugin format), generated
 * by the updateBundledLicenses task.
 */
object LicensesParser {

    private const val METADATA_FILE = "licenses/third_party_license_metadata"
    private const val LICENSES_FILE = "licenses/third_party_licenses"
    private const val LF = '\n'

    fun readLicenses(assets: AssetManager): List<License> {
        val metadata = assets.open(METADATA_FILE)
            .bufferedReader()
            .readLines()
        val licenses = assets.open(LICENSES_FILE)
            .readBytes()
        val delimiters = Regex("[: ]")
        return metadata.mapNotNull { line ->
            line.split(delimiters, limit = 3)
                .takeIf { it.size == 3 }
        }.map { (index, length, name) ->
            var start = index.toInt()
            while (licenses[start] == LF.code.toByte()) {
                start++
            }
            val text = String(licenses, index.toInt(), length.toInt(), UTF_8)
            when {
                text.contains(LF) -> License.Text(name, text)
                else -> License.Url(name, text)
            }
        }
    }
}
