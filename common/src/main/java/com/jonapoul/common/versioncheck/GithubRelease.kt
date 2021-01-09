package com.jonapoul.common.versioncheck

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
data class GithubRelease(
        @Expose
        @SerializedName("name")
        val name: String,

        @SerializedName("published_at")
        @Expose
        val publishedAt: String,

        @SerializedName("html_url")
        @Expose
        val htmlUrl: String
)
