package com.jonapoul.common.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.jonapoul.common.R
import com.jonapoul.common.di.IUiResources

class RefreshCallsignPreference(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
) : EditTextPreference(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?)
            : this(context, attrs, getDefaultAttr(context))

    constructor(context: Context)
            : this(context, null)

    init {
        isPersistent = true
        widgetLayoutResource = R.layout.refresh_callsign_preference
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val atakCallsigns = context.resources.getStringArray(R.array.atakCallsigns)
    private var refreshButton: Button? = null
    private var uiResources: IUiResources? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        with(holder.itemView) {
            refreshButton = findViewById<Button>(R.id.refresh_button)?.also { button ->
                uiResources?.let { setButtonColours(button, it) }
            }
            refreshButton?.setOnClickListener {
                val newCallsign = atakCallsigns.random()
                text = newCallsign
                prefs.edit().putString(CommonPrefs.CALLSIGN.key, newCallsign).apply()
            }
        }
    }

    fun setUiResources(uiResources: IUiResources) {
        this.uiResources = uiResources
        refreshButton?.let { setButtonColours(it, uiResources) }
    }

    private fun setButtonColours(button: Button, uiResources: IUiResources) {
        button.setBackgroundColor(ContextCompat.getColor(context, uiResources.accentColourId))
        val icon = ContextCompat.getDrawable(context, R.drawable.refresh)
        val tintedIcon = DrawableCompat.wrap(icon!!)
        DrawableCompat.setTint(tintedIcon, ContextCompat.getColor(context, R.color.black))
        button.setCompoundDrawablesWithIntrinsicBounds(tintedIcon, null, null, null)
    }
}
