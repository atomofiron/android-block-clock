package app.blockclock

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.appcompat.app.AppCompatActivity
import app.atomofiron.blockclock.BuildConfig
import app.blockclock.model.GithubAsset
import app.blockclock.model.GithubRelease
import app.blockclock.update.UpdateService
import app.blockclock.update.UpdateStore
import app.blockclock.update.model.Loading
import app.blockclock.update.model.UpdateState
import app.blockclock.update.model.UpdateType
import app.blockclock.util.Alert
import app.blockclock.util.AlertErr
import app.blockclock.util.Android
import app.blockclock.util.AppScope
import app.blockclock.util.Rslt
import app.blockclock.util.apkInfo
import app.blockclock.util.debugFailUnreachable
import app.blockclock.util.toRslt
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

private const val EXT_APK = ".apk"

class UpdateServiceGithubImpl(
    private val context: Context,
    private val scope: AppScope,
    private val api: UpdateApi,
    private val store: UpdateStore,
) : UpdateService {
    companion object : UpdateService.Factory {
        override fun new(
            context: Context,
            scope: AppScope,
            updateStore: UpdateStore,
        ): UpdateService = UpdateServiceGithubImpl(
            context,
            scope,
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
                    val rslt = installApk(file.path, UpdateService.ACTION_INSTALL_UPDATE, silently = true)
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
                        this@UpdateServiceGithubImpl.file = file
                    }
                }.let { store.set(it) }
            }
        }
    }

    private fun getFile(id: Int) = File(context.cacheDir, "updates/$id$EXT_APK")

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

    private fun installApk(path: String, action: String, stringId: String? = null, silently: Boolean = false): Rslt<Unit> = try {
        val stream = FileInputStream(path)
        val length = stream.available().toLong()
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (silently) {
            params.setAppPackageName(context.packageName)
            params.setSize(length)
            if (Android.S) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            if (Android.T) params.setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
        }
        val installer = context.packageManager.packageInstaller
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite(stringId ?: "unused", 0, length).use { output ->
                stream.use {
                    it.copyTo(output)
                    session.fsync(output)
                }
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java)
            intent.action = action
            intent.setPackage(context.packageName)
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pendingIntent.intentSender)
        }
        Rslt.Ok
    } catch (e: Exception) {
        e.toRslt()
    }
}
