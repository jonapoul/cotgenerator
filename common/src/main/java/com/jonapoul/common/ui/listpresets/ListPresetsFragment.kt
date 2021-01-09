package com.jonapoul.common.ui.listpresets

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jonapoul.common.R
import com.jonapoul.common.databinding.FragmentListPresetsBinding
import com.jonapoul.common.di.IUiResources
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.repositories.IPresetRepository
import com.jonapoul.common.ui.viewBinding
import com.jonapoul.common.utils.*
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class ListPresetsFragment : Fragment(R.layout.fragment_list_presets),
        IPresetClickListener {
    private data class PresetRecyclerInfo(val recyclerView: RecyclerView, val emptyMessage: TextView, var adapter: ListPresetsAdapter? = null)

    private val binding by viewBinding(FragmentListPresetsBinding::bind)

    private val navController by lazy { findNavController() }

    @Inject
    lateinit var presetRepository: IPresetRepository

    @Inject
    lateinit var uiResources: IUiResources

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("onViewCreated")
        setHasOptionsMenu(true)

        val viewMap = mapOf(
                Protocol.SSL to PresetRecyclerInfo(binding.sslRecyclerView, binding.sslNoneFound),
                Protocol.TCP to PresetRecyclerInfo(binding.tcpRecyclerView, binding.tcpNoneFound),
                Protocol.UDP to PresetRecyclerInfo(binding.udpRecyclerView, binding.udpNoneFound)
        )

        for ((protocol, info) in viewMap) {
            info.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            info.adapter = ListPresetsAdapter(requireContext(), this)
            info.recyclerView.adapter = info.adapter
            info.emptyMessage.visibility = View.VISIBLE
            presetRepository.getCustomByProtocol(protocol).observe(viewLifecycleOwner) { presets ->
                Timber.i("getByProtocol(${protocol}) = ${presets.size}")
                info.adapter?.updatePresets(presets)
                info.emptyMessage.visibility = if (presets.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        initialiseFab()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.d("onCreateOptionsMenu")
        menu.clear()
        inflater.inflate(R.menu.list_presets_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        if (item.itemId == R.id.delete_all) {
            MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Presets")
                    .setMessage("Remove all custom presets? The built-in defaults will still remain.")
                    .setPositiveButton(android.R.string.ok) { _, _ -> presetRepository.deleteDatabase() }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClickEditItem(preset: OutputPreset) {
        Timber.d("onClickEditItem")
        navController.safelyNavigate(uiResources.listToEditDirections(preset))
    }

    override fun onClickDeleteItem(preset: OutputPreset) {
        Timber.d("onClickDeleteItem")
        MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Preset")
                .setMessage("Are you sure you want to delete " + preset.alias + "?")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ -> presetRepository.deletePreset(preset) }
                .show()
    }

    private fun initialiseFab() {
        Timber.d("initialiseFab")
        /* Add the two items */
        binding.fab.mainFabClosedIconColor = ContextCompat.getColor(requireContext(), R.color.black)
        binding.fab.buildAndAddAction(R.id.new_preset_create, R.drawable.edit, R.string.fab_create_new_preset)
        binding.fab.buildAndAddAction(R.id.new_preset_import, R.drawable.import_file, R.string.fab_import_new_preset)

        /* React to an item being tapped */
        binding.fab.setOnActionSelectedListener {
            Timber.d("fab onActionSelected")
            binding.fab.close()
            return@setOnActionSelectedListener when (it.id) {
                R.id.new_preset_create -> {
                    /* Create new preset, so launch the activity to enter the required values */
                    navController.safelyNavigate(uiResources.listToEditDirections(null))
                    true
                }
                R.id.new_preset_import -> {
                    /* Import preset from file, so launch file browser activity */
                    showImportDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showImportDialog() {
        Timber.d("showImportDialog")
        Notify.toast(requireContext(), "Select either a .pref or a .zip file")
        MaterialFilePicker()
                .withSupportFragment(this)
                .withCloseMenu(true)
                .withPath(Paths.EXTERNAL_DIRECTORY.absolutePath)
                .withRootPath(Paths.EXTERNAL_DIRECTORY.absolutePath)
                .withFilter(Pattern.compile(".*\\.(zip|pref)$"))
                .withFilterDirectories(false)
                .withTitle("Import ATAK Preference File")
                .withRequestCode(IMPORT_FILE_REQUEST_CODE)
                .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        Timber.d("onActivityResult %d %d", requestCode, resultCode)
        super.onActivityResult(requestCode, resultCode, result)
        if (result == null || requestCode != IMPORT_FILE_REQUEST_CODE) {
            Notify.orange(requireView(), "Nothing imported!")
        } else if (resultCode != Activity.RESULT_OK) {
            Notify.red(requireView(), "Failed importing file!")
        } else {
            val path = result.getStringExtra(FilePickerActivity.RESULT_FILE_PATH) ?: return
            PresetPreferenceFileParser(requireContext(), path).parse {
                if (it == null) {
                    Notify.red(requireView(), "Failed importing from $path")
                } else {
                    navController.safelyNavigate(uiResources.listToEditDirections(it))
                }
            }
        }
    }

    private companion object {
        val IMPORT_FILE_REQUEST_CODE = GenerateInt.next()
    }

    private fun SpeedDialView.buildAndAddAction(@IdRes idRes: Int, @DrawableRes drawableRes: Int, @StringRes stringRes: Int) {
        val accent = ContextCompat.getColor(context, uiResources.accentColourId)
        val textColour = ContextCompat.getColor(context, R.color.black)
        addActionItem(
                SpeedDialActionItem.Builder(idRes, drawableRes)
                        .setLabel(stringRes)
                        .setFabBackgroundColor(textColour)
                        .setFabImageTintColor(accent)
                        .setLabelBackgroundColor(accent)
                        .setLabelColor(textColour)
                        .setLabelClickable(true)
                        .create()
        )
    }
}
