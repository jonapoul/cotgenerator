package com.jon.common.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.jon.common.AppSpecific;
import com.jon.common.presets.OutputPreset;
import com.jon.common.presets.PresetRepository;
import com.jon.common.utils.InputValidator;
import com.jon.common.utils.Key;
import com.jon.common.utils.Notify;
import com.jon.common.utils.Protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

abstract public class MainFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener,
                   Preference.OnPreferenceChangeListener {
    protected SharedPreferences prefs;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private PresetRepository repository = PresetRepository.getInstance();

    protected String[] getPhoneInputKeys() {
        return new String[] { Key.ICON_COUNT };
    }

    protected Map<String, String> getSuffixes() {
        return new HashMap<String, String>() {{
            put(Key.CENTRE_LATITUDE, "degrees");
            put(Key.CENTRE_LONGITUDE, "degrees");
        }};
    }

    protected Map<String, String> getPrefValidationRationales() {
        return new HashMap<String, String>() {{
            put(Key.CALLSIGN, "Contains invalid character(s)");
        }};
    }

    protected String[] getSeekbarKeys() {
        return new String[] {
                Key.STALE_TIMER,
                Key.TRANSMISSION_PERIOD
        };
    }

    @Override
    public void onCreatePreferences(Bundle savedState, String rootKey) {
        setPreferencesFromResource(AppSpecific.getSettingsXmlId(), rootKey);

        EditTextPreference.OnBindEditTextListener phoneInputType = (EditText text) -> text.setInputType(InputType.TYPE_CLASS_PHONE);
        for (final String key : getPhoneInputKeys()) {
            EditTextPreference pref = findPreference(key);
            if (pref != null) pref.setOnBindEditTextListener(phoneInputType);
        }

        for (final String key : getPrefValidationRationales().keySet()) {
            Preference pref = findPreference(key);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }

        for (final String key : getSeekbarKeys()) {
            SeekBarPreference seekbar = findPreference(key);
            if (seekbar != null) seekbar.setMin(1); /* I can't set the minimum in the XML for whatever reason, so here it is */
        }

        /* Launch a new activity when clicking "Edit Presets" */
        Preference editPresetsPreference = findPreference(Key.EDIT_PRESETS);
        if (editPresetsPreference != null) {
            editPresetsPreference.setOnPreferenceClickListener(clickedPref -> {
                startActivity(new Intent(getActivity(), AppSpecific.getListActivityClass()));
                return true;
            });
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle state) {
        super.onActivityCreated(state);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        updatePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        for (Map.Entry<String, String> entry : getSuffixes().entrySet()) {
            setPreferenceSuffix(prefs, entry.getKey(), entry.getValue());
        }
        updatePreferences();
    }

    protected void updatePreferences() {
        toggleProtocolSettingVisibility();
        toggleEmulateMultipleUsersSettingVisibility();

        updatePresetPreferences();
        insertPresetAddressAndPort();
    }

    @Override
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        final Map<String, String> suffixes = getSuffixes();
        if (suffixes.containsKey(key)) {
            setPreferenceSuffix(this.prefs, key, suffixes.get(key));
        }
        switch (key) {
            case Key.TRANSMISSION_PROTOCOL:
                toggleProtocolSettingVisibility();
                toggleEmulateMultipleUsersSettingVisibility();
                insertPresetAddressAndPort();
                break;
            case Key.SSL_PRESETS:
            case Key.TCP_PRESETS:
            case Key.UDP_PRESETS:
                insertPresetAddressAndPort();
                break;
        }
    }

    protected void setPrefVisibleIfCondition(String key, boolean condition) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(condition);
        }
    }

    private void toggleProtocolSettingVisibility() {
        final Protocol protocol = Protocol.fromPrefs(prefs);
        setPrefVisibleIfCondition(Key.SSL_PRESETS, protocol == Protocol.SSL);
        setPrefVisibleIfCondition(Key.TCP_PRESETS, protocol == Protocol.TCP);
        setPrefVisibleIfCondition(Key.UDP_PRESETS, protocol == Protocol.UDP);
    }

    private void toggleEmulateMultipleUsersSettingVisibility() {
        /* Can only construct multiple sockets for TCP or UDP */
        final Protocol selected = Protocol.fromPrefs(prefs);
        final boolean selectedTcpOrSsl = (selected == Protocol.TCP) || (selected == Protocol.SSL);
        setPrefVisibleIfCondition(Key.EMULATE_MULTIPLE_USERS, selectedTcpOrSsl);
    }


    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        final String input = (String) newValue;
        final String key = pref.getKey();
        switch (key) {
            case Key.CALLSIGN:
                return errorIfInvalid(input, key, InputValidator.validateCallsign(input));
            case Key.TRANSMISSION_PERIOD:
                return errorIfInvalid(input, key, InputValidator.validateInt(input, 1, null));
            default:
                return true;
        }
    }

    protected boolean errorIfInvalid(String input, String key, boolean result) {
        if (!result) {
            Notify.red(requireView(), "Invalid input: " + input + ". " + getPrefValidationRationales().get(key));
        }
        return result;
    }

    private void setPreferenceSuffix(final SharedPreferences prefs, final String key, final String suffix) {
        Preference pref = findPreference(key);
        if (pref != null) {
            String val = prefs.getString(key, "");
            pref.setSummary(String.format(Locale.ENGLISH, "%s %s", val, suffix));
        }
    }

    private void insertPresetAddressAndPort() {
        String key;
        switch (Protocol.fromPrefs(prefs)) {
            case SSL: key = Key.SSL_PRESETS; break;
            case TCP: key = Key.TCP_PRESETS; break;
            case UDP: key = Key.UDP_PRESETS; break;
            default: throw new IllegalArgumentException("Unknown protocol: " + Protocol.fromPrefs(prefs));
        }
        EditTextPreference addressPref = findPreference(Key.DEST_ADDRESS);
        EditTextPreference portPref = findPreference(Key.DEST_PORT);
        ListPreference presetPref = findPreference(key);
        if (addressPref != null && portPref != null && presetPref != null) {
            OutputPreset preset = OutputPreset.fromString(presetPref.getValue());
            if (preset != null) {
                addressPref.setText(preset.address);
                portPref.setText(Integer.toString(preset.port));
            } else {
                presetPref.setValue(null);
                addressPref.setText(null);
                portPref.setText(null);
            }
        }
    }

    private void updatePresetPreferences() {
        compositeDisposable.add(repository.getByProtocol(Protocol.SSL)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(presets -> updatePresetEntries(presets, Key.SSL_PRESETS), this::notifyError));
        compositeDisposable.add(repository.getByProtocol(Protocol.TCP)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(presets -> updatePresetEntries(presets, Key.TCP_PRESETS), this::notifyError));
        compositeDisposable.add(repository.getByProtocol(Protocol.UDP)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(presets -> updatePresetEntries(presets, Key.UDP_PRESETS), this::notifyError));
    }

    private void updatePresetEntries(List<OutputPreset> presets, String prefKey) {
        List<String> entries = OutputPreset.getAliases(presets);
        List<String> entryValues = new ArrayList<>();
        for (OutputPreset preset : presets) {
            entryValues.add(preset.toString());
        }
        ListPreference preference = findPreference(prefKey);
        if (preference != null) {
            String previousValue = preference.getValue();
            preference.setEntries(Arrays.copyOf(entries.toArray(), entries.toArray().length, String[].class));
            preference.setEntryValues(Arrays.copyOf(entryValues.toArray(), entryValues.toArray().length, String[].class));
            if (entryValues.contains(previousValue)) {
                preference.setValue(previousValue);
            } else {
                preference.setValueIndex(0);
            }
        }
    }

    private void notifyError(Throwable throwable) {
        Notify.red(requireView(), "Error: " + throwable.getMessage());
        Timber.e(throwable);
    }
}
