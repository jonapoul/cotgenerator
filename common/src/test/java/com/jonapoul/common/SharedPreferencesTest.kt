package com.jonapoul.common

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import org.mockito.Mockito

abstract class SharedPreferencesTest {
    protected lateinit var sharedPreferences: SharedPreferences

    fun initSharedPrefs() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(PreferenceManager.getDefaultSharedPreferences(context))
                .thenReturn(sharedPreferences)
    }
}
