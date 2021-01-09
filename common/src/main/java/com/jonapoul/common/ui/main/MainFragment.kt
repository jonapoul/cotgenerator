package com.jonapoul.common.ui.main

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.jonapoul.common.R
import com.jonapoul.common.databinding.FragmentMainBinding
import com.jonapoul.common.di.IUiResources
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.prefs.getStringFromPair
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.repositories.IStatusRepository
import com.jonapoul.common.service.ServiceState
import com.jonapoul.common.ui.IServiceCommunicator
import com.jonapoul.common.ui.viewBinding
import com.jonapoul.common.utils.Notify
import com.jonapoul.common.utils.Protocol
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/* Class to act as a wrapper to the SettingsFragment and the start/stop button view */
@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {

    private val serviceCommunicator by lazy { requireActivity() as IServiceCommunicator }

    @Inject
    lateinit var uiResources: IUiResources

    @Inject
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var settingsFragment: SettingsFragment

    @Inject
    lateinit var statusRepository: IStatusRepository

    private val binding by viewBinding(FragmentMainBinding::bind)

    private val startServiceOnClickListener = View.OnClickListener {
        Timber.d("Clicked start button")
        if (presetIsSelected()) {
            Timber.d("Starting service")
            serviceCommunicator.startService()
        } else {
            Notify.red(requireView(), "Select an output destination first!")
        }
    }

    private val stopServiceOnClickListener = View.OnClickListener {
        Timber.d("Clicked stop button")
        serviceCommunicator.stopService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        childFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment, settingsFragment)
                .commit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        statusRepository.getStatus().observe(viewLifecycleOwner) {
            if (it == ServiceState.RUNNING) {
                showStopButton(binding.startStopButton)
            } else {
                showStartButton(binding.startStopButton)
            }
        }
    }

    override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        Notify.setAnchor(binding.startStopButton)
    }

    override fun onPause() {
        Timber.d("onPause")
        super.onPause()
        Notify.setAnchor(null)
    }

    private fun showStopButton(button: Button) {
        Timber.d("showStopButton")
        setButtonState(
                button = button,
                textId = R.string.button_stop,
                backgroundColourId = R.color.stop_button,
                foregroundColourId = R.color.white,
                iconId = R.drawable.stop,
                onClickListener = stopServiceOnClickListener
        )
    }

    private fun showStartButton(button: Button) {
        Timber.d("showStartButton")
        setButtonState(
                button = button,
                textId = R.string.button_start,
                backgroundColourId = uiResources.accentColourId,
                foregroundColourId = R.color.black,
                iconId = R.drawable.start,
                onClickListener = startServiceOnClickListener
        )
    }

    private fun setButtonState(
            button: Button,
            @StringRes textId: Int,
            @ColorRes backgroundColourId: Int,
            @ColorRes foregroundColourId: Int,
            @DrawableRes iconId: Int,
            onClickListener: View.OnClickListener,
    ) {
        Timber.d("setButtonState")
        val context = requireContext()
        button.text = getString(textId)
        button.setBackgroundColor(ContextCompat.getColor(context, backgroundColourId))
        button.setTextColor(ContextCompat.getColor(context, foregroundColourId))
        button.setOnClickListener(onClickListener)

        val tintedIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, iconId)!!)
        DrawableCompat.setTint(tintedIcon, ContextCompat.getColor(context, foregroundColourId))
        button.setCompoundDrawablesWithIntrinsicBounds(tintedIcon, null, null, null)
    }

    private fun presetIsSelected(): Boolean {
        Timber.d("presetIsSelected")
        val presetPref = Protocol.fromPrefs(prefs).presetPref
        return !prefs.getString(CommonPrefs.DEST_ADDRESS, "").isNullOrEmpty() &&
                !prefs.getString(CommonPrefs.DEST_PORT, "").isNullOrEmpty() &&
                prefs.getStringFromPair(presetPref).split(OutputPreset.SEPARATOR).toTypedArray().isNotEmpty()
    }
}