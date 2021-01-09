package com.jonapoul.common.ui.editpreset

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavArgs
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jonapoul.common.R
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.repositories.IPresetRepository
import com.jonapoul.common.ui.BasePreferenceFragment
import com.jonapoul.common.utils.*
import com.jonapoul.sharedprefs.getStringFromPair
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject


abstract class EditPresetFragment : BasePreferenceFragment() {

    @Inject
    lateinit var presetRepository: IPresetRepository

    @Inject
    lateinit var prefs: SharedPreferences

    private val navController by lazy { findNavController() }

    private val inputValidator = InputValidator()

    protected abstract val args: NavArgs

    protected abstract fun getFragmentArgumentPreset(): OutputPreset?

    private var currentPresetId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Timber.d("onCreatePreferences")
        setPreferencesFromResource(R.xml.edit_preset, rootKey)
        super.onCreatePreferences(savedInstanceState, rootKey)

        /* Tell these two "bytes" preferences to launch a file browser when clicked */
        val clientBytesPref = findPreference<Preference>(CommonPrefs.PRESET_SSL_CLIENTCERT_BYTES.key)
        val trustBytesPref = findPreference<Preference>(CommonPrefs.PRESET_SSL_TRUSTSTORE_BYTES.key)
        clientBytesPref?.onPreferenceClickListener = fileBrowserOnClickListener(CLIENT_CERT_FILE_REQUEST_CODE, "Client")
        trustBytesPref?.onPreferenceClickListener = fileBrowserOnClickListener(TRUST_STORE_FILE_REQUEST_CODE, "Trust Store")

        /* Set all prefs requiring validation to apply checks before committing any changes */
        PREFS_REQUIRING_VALIDATION.keys.forEach { findPreference<Preference>(it)?.onPreferenceChangeListener = this }
        prepopulateSpecifiedFields()

        /* Restrict input types and set the summary as a string of asterisks */
        PASSWORD_PREF_KEYS.forEach {
            findPreference<EditTextPreference>(it)?.setOnBindEditTextListener(PASSWORD_INPUT_TYPE)
            setPasswordSummary(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.d("onCreateOptionsMenu")
        menu.clear()
        (requireActivity() as AppCompatActivity).supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.close)
        }
        inflater.inflate(R.menu.edit_preset_menu, menu)
    }

    private fun prepopulateSpecifiedFields() {
        Timber.d("prepopulateSpecifiedFields")
        getFragmentArgumentPreset()?.let { preset ->
            currentPresetId = preset.id
            findPreference<ListPreference>(CommonPrefs.PRESET_PROTOCOL.key)?.value = preset.protocol.toString()
            findPreference<EditTextPreference>(CommonPrefs.PRESET_ALIAS.key)?.text = preset.alias
            findPreference<EditTextPreference>(CommonPrefs.PRESET_DESTINATION_ADDRESS.key)?.text = preset.address
            findPreference<EditTextPreference>(CommonPrefs.PRESET_DESTINATION_PORT.key)?.text = preset.port.toString()
            populateBytesField(CommonPrefs.PRESET_SSL_CLIENTCERT_BYTES.key, preset.clientCert)
            populateBytesField(CommonPrefs.PRESET_SSL_TRUSTSTORE_BYTES.key, preset.trustStore)
            findPreference<EditTextPreference>(CommonPrefs.PRESET_SSL_CLIENTCERT_PASSWORD.key)?.text = preset.clientCertPassword
            findPreference<EditTextPreference>(CommonPrefs.PRESET_SSL_TRUSTSTORE_PASSWORD.key)?.text = preset.trustStorePassword
        }
    }

    private fun populateBytesField(key: String, bytes: ByteArray?) {
        Timber.d("populateBytesField %s", key)
        bytes?.let {
            val str = String(it)
            val length = if (inputValidator.validateString(str)) str.length else 0
            findPreference<Preference>(key)?.summary = "Loaded: $length bytes"
            prefs.edit().putString(key, str).apply()
        }
    }

    private fun fileBrowserOnClickListener(requestCode: Int, type: String) = Preference.OnPreferenceClickListener {
        Timber.d("fileBrowserOnClickListener")
        MaterialFilePicker()
                .withSupportFragment(this)
                .withCloseMenu(true)
                .withPath(Paths.EXTERNAL_DIRECTORY.absolutePath)
                .withRootPath(Paths.EXTERNAL_DIRECTORY.absolutePath)
                .withFilter(Pattern.compile(".*\\.p12$"))
                .withFilterDirectories(false)
                .withTitle("Import P12 $type Certificate")
                .withRequestCode(requestCode)
                .start()
        true
    }

    override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(this)
        toggleSSLSettingsVisibility()
    }

    override fun onPause() {
        Timber.d("onPause")
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
        clearPrefs()
    }

    @Suppress("CascadeIf")
    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        Timber.d("onActivityResult %d %d", requestCode, resultCode)
        if (result == null) {
            Notify.orange(requireView(), "Nothing imported!")
        } else if (resultCode != Activity.RESULT_OK) {
            Notify.red(requireView(), "Failed importing file!")
        } else {
            /* Get the preference key corresponding to the request code */
            val key = when (requestCode) {
                CLIENT_CERT_FILE_REQUEST_CODE -> CommonPrefs.PRESET_SSL_CLIENTCERT_BYTES.key
                TRUST_STORE_FILE_REQUEST_CODE -> CommonPrefs.PRESET_SSL_TRUSTSTORE_BYTES.key
                else -> throw RuntimeException("Unexpected request code: $requestCode")
            }

            try {
                /* Pull the picked filepath out of the intent, and store the file as bytes in shared preferences */
                val path = result.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)
                val bytes = FileUtils.toByteArray(path!!)
                prefs.edit().putString(key, String(bytes)).apply()
                findPreference<Preference>(key)?.summary = "Loaded: ${bytes.size} bytes"
            } catch (e: Exception) {
                Notify.red(requireView(), "Failed: ${e.message}")
                Timber.e(e)
                prefs.edit().remove(key).apply()
                findPreference<Preference>(key)?.summary = null
            }
            resetPassword(key)
        }
    }

    private fun resetPassword(key: String) {
        Timber.d("resetPassword %s", key)
        val passwordKey = when (key) {
            CommonPrefs.PRESET_SSL_CLIENTCERT_BYTES.key -> CommonPrefs.PRESET_SSL_CLIENTCERT_PASSWORD.key
            CommonPrefs.PRESET_SSL_TRUSTSTORE_BYTES.key -> CommonPrefs.PRESET_SSL_TRUSTSTORE_PASSWORD.key
            else -> throw IllegalArgumentException("Unexpected key $key")
        }
        findPreference<EditTextPreference>(passwordKey)?.let {
            it.text = null
            it.summary = null
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Timber.d("onSharedPreferenceChanged %s", key)
        when (key) {
            CommonPrefs.PRESET_SSL_CLIENTCERT_PASSWORD.key, CommonPrefs.PRESET_SSL_TRUSTSTORE_PASSWORD.key -> setPasswordSummary(key)
            CommonPrefs.PRESET_PROTOCOL.key -> toggleSSLSettingsVisibility()
        }
    }

    private fun setPasswordSummary(key: String) {
        Timber.d("setPasswordSummary %s", key)
        findPreference<EditTextPreference>(key)?.let {
            val length = prefs.getString(key, "")!!.length
            it.summary = "*".repeat(length)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        Timber.d("onPreferenceChange %s", preference.key)
        val input = newValue as String
        return when (val key = preference.key) {
            CommonPrefs.PRESET_DESTINATION_ADDRESS.key -> errorIfInvalid(input, key, inputValidator.validateHostname(input))
            CommonPrefs.PRESET_DESTINATION_PORT.key -> errorIfInvalid(input, key, inputValidator.validateInt(input, 1, 65535))
            else -> true
        }
    }

    private fun errorIfInvalid(input: String, key: String, result: Boolean): Boolean {
        if (!result) {
            Notify.red(requireView(), "Invalid input: $input. ${PREFS_REQUIRING_VALIDATION[key]}")
        }
        return result
    }

    private fun setCategoryVisibleIfCondition(key: String, condition: Boolean) {
        Timber.d("setCategoryVisibleIfCondition %s %s", key, condition)
        findPreference<Preference>(key)?.let {
            it.isVisible = condition
        }
    }

    private fun toggleSSLSettingsVisibility() {
        val sslIsSelected = try {
            Protocol.fromString(prefs.getStringFromPair(CommonPrefs.PRESET_PROTOCOL)) == Protocol.SSL
        } catch (e: Exception) {
            /* thrown if the selected value is null */
            false
        }
        Timber.d("toggleSSLSettingsVisibility %s", sslIsSelected)
        setCategoryVisibleIfCondition(CommonPrefs.PRESET_SSL_OPTIONS_CATEGORY, sslIsSelected)
    }

    @SuppressLint("ApplySharedPref")
    private fun clearPrefs() {
        Timber.d("clearPrefs")
        val editor = prefs.edit()
        ALL_TEXT_PREFS.forEach { key ->
            editor.remove(key)
            findPreference<EditTextPreference>(key)?.let { it.text = null }
        }
        ALL_BYTES_PREFS.forEach { key ->
            editor.remove(key)
            findPreference<Preference>(key)?.let { it.summary = null }
        }
        editor.remove(CommonPrefs.PRESET_PROTOCOL.key)
        findPreference<ListPreference>(CommonPrefs.PRESET_PROTOCOL.key)?.value = null
        editor.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        val resId = item.itemId
        if (resId == android.R.id.home) {
            onBackPressed()
        } else if (resId == R.id.save && settingsAreValid()) {
            storePresetInDatabase()
            Notify.green(requireView(), "Preset saved!")
            navController.navigateUp()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onBackPressed() {
        Timber.d("onBackPressed")
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.go_back)
                .setMessage(R.string.go_back_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> navController.navigateUp() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun settingsAreValid(): Boolean {
        Timber.d("settingsAreValid")
        val inputValidator = InputValidator()
        try {
            /* Regular options */
            val protocolStr = prefs.getStringFromPair(CommonPrefs.PRESET_PROTOCOL)
            val alias = prefs.getStringFromPair(CommonPrefs.PRESET_ALIAS)
            val address = prefs.getStringFromPair(CommonPrefs.PRESET_DESTINATION_ADDRESS)
            val portStr = prefs.getStringFromPair(CommonPrefs.PRESET_DESTINATION_PORT)
            if (!inputValidator.validateString(protocolStr, ".*?")) {
                Notify.orange(requireView(), "No protocol chosen!")
                return false
            } else if (!inputValidator.validateString(alias, ".*?")) {
                Notify.orange(requireView(), "Enter an alias to describe the preset!")
                return false
            } else if (!inputValidator.validateString(address, ".*?")) {
                Notify.orange(requireView(), "No destination address has been entered!")
                return false
            } else if (!inputValidator.validateInt(portStr, 1, 65535)) {
                Notify.orange(requireView(), "Invalid port number!")
                return false
            }
            if (getInputProtocol() == Protocol.SSL) {
                /* SSL-only options */
                val clientBytes = prefs.getStringFromPair(CommonPrefs.PRESET_SSL_CLIENTCERT_BYTES)
                val clientPass = prefs.getStringFromPair(CommonPrefs.PRESET_SSL_CLIENTCERT_PASSWORD)
                val trustBytes = prefs.getStringFromPair(CommonPrefs.PRESET_SSL_TRUSTSTORE_BYTES)
                val trustPass = prefs.getStringFromPair(CommonPrefs.PRESET_SSL_TRUSTSTORE_PASSWORD)
                if (!inputValidator.validateString(clientBytes)) {
                    Notify.orange(requireView(), "No client certificate chosen!")
                    return false
                } else if (!inputValidator.validateString(clientPass)) {
                    Notify.orange(requireView(), "No client certificate password entered!")
                    return false
                } else if (!inputValidator.validateString(trustBytes)) {
                    Notify.orange(requireView(), "No trust store chosen!")
                    return false
                } else if (!inputValidator.validateString(trustPass)) {
                    Notify.orange(requireView(), "No trust store password entered!")
                    return false
                }
            }
        } catch (e: Exception) {
            Notify.orange(requireView(), "Unknown error when validating preset: ${e.message}")
            return false
        }
        return true
    }

    private fun getEnteredPresetValues(): OutputPreset {
        Timber.d("getEnteredPresetValues")
        val protocol = getInputProtocol()
        val preset = OutputPreset(
                protocol,
                prefs.getStringFromPair(CommonPrefs.PRESET_ALIAS),
                prefs.getStringFromPair(CommonPrefs.PRESET_DESTINATION_ADDRESS),
                prefs.getStringFromPair(CommonPrefs.PRESET_DESTINATION_PORT).toInt()
        )
        if (protocol == Protocol.SSL) {
            preset.clientCert = prefs.getStringFromPair(CommonPrefs.PRESET_SSL_CLIENTCERT_BYTES).toByteArray()
            preset.trustStore = prefs.getStringFromPair(CommonPrefs.PRESET_SSL_TRUSTSTORE_BYTES).toByteArray()
            preset.clientCertPassword = prefs.getStringFromPair(CommonPrefs.PRESET_SSL_CLIENTCERT_PASSWORD)
            preset.trustStorePassword = prefs.getStringFromPair(CommonPrefs.PRESET_SSL_TRUSTSTORE_PASSWORD)
        }
        if (currentPresetId != 0) {
            /* If this is an edit of an existing preset, attach its unique ID number */
            preset.id = currentPresetId
        }
        return preset
    }

    private fun storePresetInDatabase() {
        Timber.d("storePresetInDatabase")
        presetRepository.insertPreset(getEnteredPresetValues())
    }

    private fun getInputProtocol() = Protocol.fromString(prefs.getStringFromPair(CommonPrefs.PRESET_PROTOCOL))

    companion object {
        private val CLIENT_CERT_FILE_REQUEST_CODE = GenerateInt.next()
        private val TRUST_STORE_FILE_REQUEST_CODE = GenerateInt.next()

        private val ALL_TEXT_PREFS = arrayOf(
                CommonPrefs.PRESET_ALIAS.key,
                CommonPrefs.PRESET_DESTINATION_ADDRESS.key,
                CommonPrefs.PRESET_DESTINATION_PORT.key,
                CommonPrefs.PRESET_SSL_CLIENTCERT_PASSWORD.key,
                CommonPrefs.PRESET_SSL_TRUSTSTORE_PASSWORD.key
        )

        private val ALL_BYTES_PREFS = arrayOf(
                CommonPrefs.PRESET_SSL_CLIENTCERT_BYTES.key,
                CommonPrefs.PRESET_SSL_TRUSTSTORE_BYTES.key
        )

        private val PASSWORD_INPUT_TYPE = EditTextPreference.OnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        private val PASSWORD_PREF_KEYS = arrayOf(
                CommonPrefs.PRESET_SSL_CLIENTCERT_PASSWORD.key,
                CommonPrefs.PRESET_SSL_TRUSTSTORE_PASSWORD.key
        )
        private val PREFS_REQUIRING_VALIDATION = hashMapOf(
                CommonPrefs.PRESET_DESTINATION_ADDRESS.key to "Should be a valid network address",
                CommonPrefs.PRESET_DESTINATION_PORT.key to "Should be an integer from 1-65355 inclusive"
        )
    }
}