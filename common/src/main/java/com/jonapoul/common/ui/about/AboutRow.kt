package com.jonapoul.common.ui.about

import androidx.annotation.DrawableRes

internal data class AboutRow(val title: String, var subtitle: String, @DrawableRes var iconId: Int? = null)
