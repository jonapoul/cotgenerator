package com.jonapoul.common.cot

import com.jonapoul.common.SharedPreferencesTest
import com.jonapoul.common.prefs.CommonPrefs
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test

class CotRoleTest : SharedPreferencesTest() {
    private val allRoles = listOf(*CotRole.values())

    @Before
    fun before() {
        initSharedPrefs()
    }

    @Test
    fun get_Valid() {
        assertThat(CotRole.TEAM_MEMBER.toString(), equalTo("Team Member"))
        assertThat(CotRole.TEAM_LEADER.toString(), equalTo("Team Leader"))
        assertThat(CotRole.HQ.toString(), equalTo("HQ"))
        assertThat(CotRole.SNIPER.toString(), equalTo("Sniper"))
        assertThat(CotRole.MEDIC.toString(), equalTo("Medic"))
        assertThat(CotRole.FORWARD_OBSERVER.toString(), equalTo("Forward Observer"))
        assertThat(CotRole.RTO.toString(), equalTo("RTO"))
        assertThat(CotRole.K9.toString(), equalTo("K9"))
    }

    @Test
    fun fromString_Valid() {
        val role = CotRole.fromString(CotRole.MEDIC.toString())
        assertThat(allRoles, hasItem(role))
    }

    @Test
    fun fromString_AllValid() {
        for (role in CotRole.values()) {
            val parsedRole = CotRole.fromString(role.toString())
            assertThat(allRoles, hasItem(parsedRole))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromString_Invalid() {
        CotRole.fromString("INVALID ROLE")
    }

    @Test
    fun fromPrefs_RandomValid() {
        initSharedPrefs()
        val role = CotRole.fromPrefs(sharedPreferences, isRandom = true)
        assertThat(allRoles, hasItem(role))
    }

    @Test
    fun fromPrefs_RandomAllValid() {
        initSharedPrefs()
        for (i in 0..49) {
            val role = CotRole.fromPrefs(sharedPreferences, isRandom = true)
            assertThat(allRoles, hasItem(role))
        }
    }

    @Test
    fun fromPrefs_SpecificValid() {
        initSharedPrefs()
        allRoles.forEach { role ->
            sharedPreferences.edit()
                    .putString(CommonPrefs.ICON_ROLE.key, role.toString())
                    .commit()
            val parsed = CotRole.fromPrefs(sharedPreferences, isRandom = false)
            assertThat(parsed, equalTo(role))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromPrefs_SpecificInvalid() {
        initSharedPrefs()
        sharedPreferences.edit()
                .putString(CommonPrefs.ICON_ROLE.key, "INVALID ROLE")
                .commit()
        CotRole.fromPrefs(sharedPreferences, false)
    }
}
