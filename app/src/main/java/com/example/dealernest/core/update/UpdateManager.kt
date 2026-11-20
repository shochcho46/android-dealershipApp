package com.example.dealernest.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.dealernest.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** What the app needs to know about a newer GitHub release. */
data class AvailableUpdate(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
)

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val url: String = "",
)

/**
 * Self-update via GitHub Releases (no CI): upload an .apk to a release whose tag is the new
 * version (e.g. `v1.0.1`) and every installed copy offers it on next launch.
 *
 * Uses its own plain [OkHttpClient] on purpose - the app's shared client carries
 * AuthInterceptor, which would leak the user's API Bearer token to github.com.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val client = OkHttpClient()

    /** Returns the newer release, or null when up to date / offline / no APK attached. */
    suspend fun checkForUpdate(): AvailableUpdate? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            val body = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                resp.body.string()
            }
            val release = json.decodeFromString<GithubRelease>(body)
            if (release.draft || release.prerelease) return@runCatching null
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return@runCatching null
            if (!isNewer(release.tagName, BuildConfig.VERSION_NAME)) return@runCatching null
            AvailableUpdate(
                versionName = release.tagName.removePrefix("v").removePrefix("V"),
                notes = release.body.orEmpty().trim(),
                apkUrl = apk.url,
            )
        }.getOrNull()
    }

    /** Downloads the APK to cache, reporting progress 0f..1f. Returns the file, or null on failure. */
    suspend fun download(update: AvailableUpdate, onProgress: (Float) -> Unit): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
                val target = File(dir, "dealernest-${update.versionName}.apk")
                client.newCall(Request.Builder().url(update.apkUrl).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val total = resp.body.contentLength()
                    resp.body.byteStream().use { input ->
                        target.outputStream().use { out ->
                            val buf = ByteArray(16 * 1024)
                            var done = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                if (total > 0) onProgress(done.toFloat() / total)
                            }
                        }
                    }
                }
                target
            }.getOrNull()
        }

    /**
     * Opens the system installer. Returns false if the user must first allow "install unknown
     * apps" for this app (we open that settings page; they then tap Install again).
     */
    fun install(apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }

    companion object {
        /** GitHub "owner/repo" that hosts the releases. */
        const val REPO = "shochcho46/android-dealershipApp"

        /** True when [remote] (e.g. "v1.2.0") is a higher version than [local] (e.g. "1.1.9"). */
        fun isNewer(remote: String, local: String): Boolean {
            fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V")
                .split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            val r = parts(remote)
            val l = parts(local)
            for (i in 0 until maxOf(r.size, l.size)) {
                val a = r.getOrElse(i) { 0 }
                val b = l.getOrElse(i) { 0 }
                if (a != b) return a > b
            }
            return false
        }
    }
}
