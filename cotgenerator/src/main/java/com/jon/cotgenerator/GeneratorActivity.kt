package com.jon.cotgenerator

import android.view.MenuItem
import com.jon.common.ui.main.MainActivity
import com.jon.common.utils.safelyNavigate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeneratorActivity : MainActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.location ->
                navController.safelyNavigate(activityResources.mainToLocationDirections)
            R.id.about ->
                navController.safelyNavigate(activityResources.mainToAboutDirections)
        }
        return super.onOptionsItemSelected(item)
    }
}
