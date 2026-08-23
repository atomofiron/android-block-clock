package app.atomofiron.blockclock

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import app.atomofiron.blockclock.model.GithubAsset
import app.atomofiron.blockclock.model.GithubRelease
import app.atomofiron.blockclock.update.UpdateService
import app.atomofiron.blockclock.update.UpdateStore
import app.atomofiron.blockclock.update.model.Loading
import app.atomofiron.blockclock.update.model.UpdateState
import app.atomofiron.blockclock.update.model.UpdateType
import app.atomofiron.blockclock.util.Alert
import app.atomofiron.blockclock.util.AlertErr
import app.atomofiron.blockclock.util.AppScope
import app.atomofiron.blockclock.util.Rslt
import app.atomofiron.blockclock.util.apkInfo
import app.atomofiron.blockclock.util.debugFailUnreachable
import app.blockclock.R
import kotlinx.coroutines.launch
import java.io.File

private const val EXT_APK = ".apk"
private const val SUBDIR = "updates"

class AppUpdateServiceGithubImpl(
    private val context: Context,
    private val scope: AppScope,
    private val apks: ApkService,
    private val api: UpdateApi,
    private val store: UpdateStore,
) : UpdateService {
    companion object : UpdateService.Factory {
        override fun new(
            context: Context,
            scope: AppScope,
            updateStore: UpdateStore,
        ): UpdateService = AppUpdateServiceGithubImpl(
            context,
            scope,
            ApkService(context, context.packageManager.packageInstaller),
            UpdateApi(),
            updateStore,
        )
    }

    private var asset: GithubAsset? = null
    private var file: File? = null

    override fun onActivityCreate(activity: AppCompatActivity) = Unit

    override fun check(userAction: Boolean) {
        store.set(UpdateState.Checking)
        scope.launch {
            when (val releases = api.releases()) {
                is Rslt.Ok -> releases.value
                    .findAsset(userAction)
                    .also { asset = it }
                    ?.checkFile()
                    ?: UpdateState.UpToDate.also {
                        if (userAction) store.showUpdateAlert(Alert(context.getString(R.string.is_up_to_date)))
                    }
                is Rslt.Err -> {
                    store.showUpdateAlert(AlertErr(releases.message))
                    UpdateState.Unknown
                }
            }.let { store.set(it) }
        }
    }

    override fun retry() = when (val state = asset?.checkFile()) {
        null -> check()
        else -> store.set(state)
    }

    override fun startUpdate(variant: UpdateType.Variant) {
        val asset = asset ?: return debugFailUnreachable()
        file = getFile(asset.id).verify(asset)
        when (file) {
            null -> downloadUpdate(asset)
            else -> store.set(UpdateState.Completable)
        }
    }

    override fun completeUpdate() {
        when (val file = file) {
            null -> store.set(asset?.checkFile() ?: UpdateState.Unknown)
            else -> {
                val state = store.state.value
                store.set(UpdateState.Installing)
                scope.launch {
                    val rslt = apks.installApk(file.path, UpdateService.ACTION_INSTALL_UPDATE, silently = true)
                    if (rslt is Rslt.Err) {
                        store.set(state)
                        store.showUpdateAlert(AlertErr(rslt.message))
                    }
                }
            }
        }
    }

    private fun List<GithubRelease>.findAsset(userAction: Boolean) = filter { release -> release.assets.any { it.name.endsWith(EXT_APK) } }
        .maxByOrNull { it.publishedAt }
        ?.takeIf { it.isNewerThan(BuildConfig.UPDATE_THRESHOLD) || userAction && BuildConfig.DEBUG }
        ?.assets
        ?.firstOrNull { it.name.endsWith(EXT_APK) }

    private fun GithubAsset.checkFile(): UpdateState {
        file = getFile(id).verify(this)
        return when (file) {
            null -> UpdateState.Available(UpdateType.Flexible, id)
            else -> UpdateState.Completable
        }
    }

    private fun downloadUpdate(asset: GithubAsset) {
        scope.launch {
            val file = getFile(asset.id)
            file.delete()
            val fallback = store.state.value
            api.download(asset.browserDownloadUrl, file).collect { downloading ->
                when (downloading) {
                    is Loading.Error -> fallback
                    is Loading.Progress -> UpdateState.Downloading(downloading.progress)
                    is Loading.Completed -> UpdateState.Completable.also {
                        this@AppUpdateServiceGithubImpl.file = file
                    }
                }.let { store.set(it) }
            }
        }
    }

    private fun getFile(id: Int) = File(context.cacheDir, "$SUBDIR/$id$EXT_APK")

    private fun File.verify(asset: GithubAsset): File? = when {
        !exists() -> false
        length() != asset.size -> false
        BuildConfig.DEBUG -> true
        else -> context.packageManager.apkInfo(path, icon = false)
            ?.let { it.versionCode > BuildConfig.VERSION_CODE }== true
    }.let { verified ->
        if (!verified) delete()
        takeIf { verified }
    }
}
