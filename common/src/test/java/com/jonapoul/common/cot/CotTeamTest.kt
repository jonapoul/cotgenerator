package com.jonapoul.common.cot

import com.jonapoul.common.SharedPreferencesTest
import com.jonapoul.common.prefs.CommonPrefs
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test

@Suppress("PrivatePropertyName")
class CotTeamTest : SharedPreferencesTest() {
    private val ALL_TEAMS = listOf(*CotTeam.values())

    @Before
    fun before() {
        initSharedPrefs()
    }

    @Test
    fun fromPrefs_RandomValid() {
        val team = CotTeam.fromPrefs(sharedPreferences, isRandom = true)
        assertThat(ALL_TEAMS, hasItem(team))
    }

    @Test
    fun fromPrefs_RandomAllValid() {
        for (i in 0..100) {
            val team = CotTeam.fromPrefs(sharedPreferences, isRandom = true)
            assertThat(ALL_TEAMS, hasItem(team))
        }
    }

    @Test
    fun fromPrefs_SpecificValid() {
        sharedPreferences.edit()
                .putInt(CommonPrefs.TEAM_COLOUR.key, colourToInteger(CotTeam.CYAN))
                .commit()
        val team = CotTeam.fromPrefs(sharedPreferences, isRandom = false)
        assertThat(team, equalTo(CotTeam.CYAN))
        assertThat(team.toString(), equalTo("Cyan"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromPrefs_SpecificInvalid() {
        sharedPreferences.edit()
                .putInt(CommonPrefs.TEAM_COLOUR.key, 0x000000) // black, not an option
                .commit()
        CotTeam.fromPrefs(sharedPreferences, isRandom = false)
    }

    private fun colourToInteger(team: CotTeam): Int {
        return team.toHexString().substring(2).toInt(16)
    }
}
