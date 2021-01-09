package com.jonapoul.cotbeacon.ui

import android.view.MenuItem
import androidx.lifecycle.ViewModel
import com.jonapoul.cotbeacon.R
import com.jonapoul.cotbeacon.cot.EmergencyType
import javax.inject.Inject

class BeaconActivityViewModel @Inject constructor() : ViewModel() {
    var emergencyIsActive = false
        private set

    fun setEmergencyState(emergencyType: EmergencyType) {
        emergencyIsActive = emergencyType != EmergencyType.CANCEL
    }

    fun setEmergencyMenuItemState(emergencyMenuItem: MenuItem?) {
        emergencyMenuItem?.setIcon(
                if (emergencyIsActive) {
                    R.drawable.emergency_active
                } else {
                    R.drawable.emergency_not_active
                }
        )
    }
}
