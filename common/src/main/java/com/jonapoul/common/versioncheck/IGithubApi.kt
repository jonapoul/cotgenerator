package com.jonapoul.common.versioncheck

import io.reactivex.Observable
import retrofit2.http.GET

interface IGithubApi {
    @GET("/repos/jonapoul/cotgenerator/releases")
    fun getAllReleases(): Observable<List<GithubRelease>>
}
