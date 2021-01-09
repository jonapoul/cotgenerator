package com.jonapoul.cotbeacon.ui

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jonapoul.cotbeacon.R
import com.jonapoul.cotbeacon.cot.EmergencyType
import com.jonapoul.cotbeacon.databinding.EmergencyDialogBinding

internal class EmergencyDialogBuilder(
        context: Context,
        emergencyIsActive: Boolean,
        callback: (EmergencyType) -> Unit,
) : MaterialAlertDialogBuilder(context) {

    private val binding = EmergencyDialogBinding.inflate(LayoutInflater.from(context), null, false)

    init {
        binding.spinner.setItems(EmergencyType.values().map { it.description })

        val defaultSelection = if (emergencyIsActive) {
            EmergencyType.CANCEL
        } else {
            EmergencyType.ALERT_911
        }
        binding.spinner.selectedIndex = indexOf(defaultSelection)

        setTitle(R.string.emergency_dialog_title)
        setView(binding.root)
        setNegativeButton(R.string.emergency_back, null)
        setPositiveButton(R.string.emergency_send) { dialog, _ ->
            callback(getSelectedEmergencyType())
            dialog.dismiss()
        }
    }

    private fun getSelectedEmergencyType(): EmergencyType {
        return EmergencyType.values()[binding.spinner.selectedIndex]
    }

    private fun indexOf(emergencyType: EmergencyType): Int {
        return EmergencyType.values().indexOf(emergencyType)
    }
}
