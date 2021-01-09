package com.jonapoul.cotgenerator.ui

import android.view.MenuItem
import com.jonapoul.common.ui.main.MainActivity
import com.jonapoul.common.utils.safelyNavigate
import com.jonapoul.cotgenerator.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeneratorActivity : MainActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.location ->
                navController.safelyNavigate(uiResources.mainToLocationDirections)
            R.id.about ->
                navController.safelyNavigate(uiResources.mainToAboutDirections)
        }
        return super.onOptionsItemSelected(item)
    }
}
