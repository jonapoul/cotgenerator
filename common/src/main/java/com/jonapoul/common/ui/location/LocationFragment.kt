package com.jonapoul.common.ui.location

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.jonapoul.common.R
import com.jonapoul.common.databinding.FragmentLocationBinding
import com.jonapoul.common.di.IUiResources
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.ui.viewBinding
import com.jonapoul.common.utils.MinimumVersions.GNSS_CALLBACK
import com.jonapoul.common.utils.Notify
import com.jonapoul.common.utils.VersionUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class LocationFragment : Fragment(R.layout.fragment_location),
        SensorEventListener,
        GnssCallback.IListener {

    private val binding by viewBinding(FragmentLocationBinding::bind)

    private val locationManager by lazy { requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val gpsConverter = GpsConverter()
    private val compass by lazy { Compass(requireContext()) }
    private lateinit var coordinateFormat: CoordinateFormat

    private var gnssCallback: GnssCallback? = null
    private var mostRecentLocation: Location? = null

    @Inject
    lateinit var gpsRepository: IGpsRepository

    @Inject
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var uiResources: IUiResources

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        initialiseCoordinateFormat()
        initialiseCoordinateButtons()
        observeGpsData()
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            binding.numSatellites.text = GPS_NOT_ENABLED
        }
    }

    override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        compass.registerListener(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        Timber.d("onStart")
        super.onStart()
        if (VersionUtils.isAtLeast(GNSS_CALLBACK)) {
            gnssCallback = GnssCallback(this).also {
                locationManager.registerGnssStatusCallback(it)
            }
        }
    }

    override fun onStop() {
        Timber.d("onStop")
        super.onStop()
        if (VersionUtils.isAtLeast(GNSS_CALLBACK)) {
            gnssCallback?.let { locationManager.unregisterGnssStatusCallback(it) }
        }
    }

    override fun onPause() {
        Timber.d("onPause")
        super.onPause()
        compass.unregisterListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.d("onCreateOptionsMenu")
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent?) {
        Timber.d("onSensorChanged %s", event)
        if (event == null || !compass.shouldRecalculate()) {
            return
        }
        compass.getCompassReading(event).also {
            binding.compassDegrees.text = "%3.0f° %s".format(it.degrees, it.direction)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        /* No-op */
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onUsefulSatellitesReported(numUsefulSatellites: Int) {
        Timber.d("onUsefulSatellitesReported %d", numUsefulSatellites)
        binding.numSatellites.text = if (numUsefulSatellites == GnssCallback.GNSS_STOPPED) {
            GPS_NOT_ENABLED
        } else {
            numUsefulSatellites.toString()
        }
    }

    private fun initialiseCoordinateFormat() {
        Timber.d("initialiseCoordinateFormat")
        coordinateFormat = CoordinateFormat.fromPrefs(prefs)
        showCorrectCoordinateViews()
    }

    private fun initialiseCoordinateButtons() {
        Timber.d("initialiseCoordinateButtons")
        val accent = ContextCompat.getColor(requireContext(), uiResources.accentColourId)
        binding.coordFormatButton.setBackgroundColor(accent)
        binding.coordFormatButton.text = coordinateFormat.name
        binding.coordFormatButton.setOnClickListener {
            Timber.d("coordFormatButton clicked")
            coordinateFormat = CoordinateFormat.getNext(coordinateFormat)
            binding.coordFormatButton.text = coordinateFormat.name
            convertAndDisplayCoordinates(mostRecentLocation)
            showCorrectCoordinateViews()
            prefs.edit()
                    .putString(CommonPrefs.LOCATION_COORDINATE_FORMAT.key, coordinateFormat.name)
                    .apply()
        }

        binding.coordCopyButton.setBackgroundColor(accent)
        val tintedIcon = DrawableCompat.wrap(ContextCompat.getDrawable(requireContext(), R.drawable.copy)!!)
        DrawableCompat.setTint(tintedIcon, ContextCompat.getColor(requireContext(), R.color.black))
        binding.coordCopyButton.setCompoundDrawablesWithIntrinsicBounds(tintedIcon, null, null, null)
        binding.coordCopyButton.setOnClickListener {
            Timber.d("coordCopyButton clicked")
            /* Convert the displayed coordinates to a string and place it in the clipboard */
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val coordsString = gpsConverter.coordinatesToCopyableString(mostRecentLocation, coordinateFormat)
            val clip = ClipData.newPlainText("Copied ${coordinateFormat.name} coordinates", coordsString)
            clipboard.setPrimaryClip(clip)
            Notify.green(requireView(), "Copied \"${coordsString}\" to the clipboard")
        }
    }

    private fun observeGpsData() {
        gpsRepository.getLocation().observe(viewLifecycleOwner) { location ->
            Timber.d("Location data updated")
            mostRecentLocation = location
            convertAndDisplayCoordinates(location)
        }
    }

    private fun convertAndDisplayCoordinates(location: Location?) {
        Timber.d("convertAndDisplayCoordinates")
        gpsConverter.convertCoordinates(location, coordinateFormat).also {
            binding.latDegrees.text = it.latitude
            binding.lonDegrees.text = it.longitude
            binding.mgrs.text = it.mgrs
            binding.positionalError.text = it.positionalError
            binding.altitudeMetres.text = it.altitudeWgs84
            binding.speed.text = it.speedMetresPerSec
            binding.bearing.text = it.bearing
        }
    }

    private fun showCorrectCoordinateViews() {
        Timber.d("showCorrectCoordinateViews %s", coordinateFormat)
        when (coordinateFormat) {
            CoordinateFormat.MGRS ->
                toggleViewVisibility(
                        visible = listOf(binding.mgrsLayout),
                        hidden = listOf(binding.latitudeLayout, binding.longitudeLayout)
                )
            else ->
                toggleViewVisibility(
                        visible = listOf(binding.latitudeLayout, binding.longitudeLayout),
                        hidden = listOf(binding.mgrsLayout)
                )
        }
    }

    private fun toggleViewVisibility(visible: List<View>, hidden: List<View>) {
        Timber.d("toggleViewVisibility")
        visible.forEach { it.visibility = View.VISIBLE }
        hidden.forEach { it.visibility = View.GONE }
    }

    private companion object {
        const val GPS_NOT_ENABLED = "GPS NOT ENABLED"
    }
}