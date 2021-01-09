package com.jonapoul.common.versioncheck

import android.content.SharedPreferences
import com.jonapoul.common.cot.UtcTimestamp
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.prefs.CommonPrefs
import io.reactivex.Observable
import javax.inject.Inject
import kotlin.math.max

class UpdateChecker @Inject constructor(
        private val githubApi: IGithubApi?,
        private val buildResources: IBuildResources
) {
    fun fetchReleases(): Observable<List<GithubRelease>> {
        return githubApi!!.getAllReleases()
    }

    fun getLatestRelease(releases: List<GithubRelease>?): GithubRelease? {
        return releases?.maxByOrNull { UtcTimestamp(it.publishedAt).milliseconds() }
    }

    fun isNewerVersion(latest: GithubRelease): Boolean {
        return try {
            /* Split into arrays of major, minor version numbers */
            val discovered = latest.name.split(".").map { it.toInt() }.toMutableList()
            val installed = buildResources.versionName.split(".").map { it.toInt() }.toMutableList()
            val longest = max(discovered.size, installed.size)

            /* Make them both the same length, padded with trailing zeros. Accounts for comparisons between
            * versions like "1.3.2" and "1.4"  */
            discovered += List(longest - discovered.size) { 0 }
            installed += List(longest - installed.size) { 0 }

            for (i in 0..longest) {
                if (discovered[i] > installed[i]) {
                    return true
                } else if (discovered[i] < installed[i]) {
                    return false
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun releaseIsNotIgnored(release: GithubRelease, prefs: SharedPreferences): Boolean {
        val ignored = getPreviouslyIgnoredVersions(prefs)
        return ignored == null || !ignored.contains(release.name)
    }

    fun ignoreRelease(release: GithubRelease, prefs: SharedPreferences) {
        val allIgnoredVersions = mutableSetOf(release.name)
        getPreviouslyIgnoredVersions(prefs)?.let { allIgnoredVersions.addAll(it) }
        prefs.edit()
                .putStringSet(CommonPrefs.IGNORED_UPDATE_VERSIONS, allIgnoredVersions)
                .apply()
    }

    private fun getPreviouslyIgnoredVersions(prefs: SharedPreferences): Set<String>? {
        return prefs.getStringSet(CommonPrefs.IGNORED_UPDATE_VERSIONS, null)
    }
}
