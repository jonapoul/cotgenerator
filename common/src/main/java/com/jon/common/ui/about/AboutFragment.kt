package com.jon.common.ui.about

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.jon.common.R
import com.jon.common.variants.Variant
import com.jon.common.versioncheck.GithubRelease
import com.jon.common.versioncheck.UpdateChecker
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.*


class AboutFragment : Fragment() {
    private val updateChecker = UpdateChecker()

    private val rows: List<AboutRow> = listOf(
            AboutRow("Version", Variant.getVersionName()),
            AboutRow("Build Type", BUILD_TYPE),
            AboutRow("Build Time", BUILD_DATE),
            AboutRow("Latest Github Release", LOADING, R.drawable.refresh),
            AboutRow("Github Repository", "https://github.com/jonapoul/cotgenerator", R.drawable.go_to)
    )

    private val adapter by lazy { AboutArrayAdapter(requireContext(), rows) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val listView = View.inflate(context, R.layout.fragment_about_listview, null) as ListView
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position: Int, _ ->
            if (position == GITHUB_INDEX) {
                /* Open a web browser to the Github page */
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(rows[position].subtitle)
                requireContext().startActivity(intent)
            } else if (position == LATEST_INDEX) {
                /* Check for any new versions */
                rows[LATEST_INDEX].subtitle = LOADING
                adapter.notifyDataSetChanged()
                fetchLatestVersion()
            }
        }
        return listView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchLatestVersion()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    @SuppressLint("CheckResult")
    private fun fetchLatestVersion() {
        updateChecker.fetchReleases()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { updateChecker.getLatestRelease(it) }
                .subscribe(::onSuccess, ::onFailure)
    }

    private fun onSuccess(release: GithubRelease?) {
        if (release != null) {
            val suffix = getVersionSuffix(release)
            rows[LATEST_INDEX].subtitle = release.name + suffix
            adapter.notifyDataSetChanged()
        } else {
            onFailure(Exception("Null response"))
        }
    }

    private fun onFailure(throwable: Throwable) {
        rows[LATEST_INDEX].subtitle = "[Error: ${throwable.message}]"
        adapter.notifyDataSetChanged()
    }

    private fun getVersionSuffix(latest: GithubRelease): String {
        return if (updateChecker.isNewerVersion(latest)) {
            " - Update available!"
        } else {
            " - You're up-to-date!"
        }
    }

    private companion object {
        const val LOADING = "Loading..."
        const val LATEST_INDEX = 3
        const val GITHUB_INDEX = 4

        private val BUILD_TYPE = if (Variant.isDebug()) "Debug" else "Release"
        private val BUILD_DATE = SimpleDateFormat("HH:mm:ss dd MMM yyyy z", Locale.ENGLISH).format(Variant.getBuildDate())
    }
}