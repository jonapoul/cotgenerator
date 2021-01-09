package com.jonapoul.common.ui.about

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jonapoul.common.R

internal class AboutArrayAdapter(
        context: Context,
        private val rows: List<AboutRow>
) : ArrayAdapter<AboutRow>(context, R.layout.fragment_about_row, R.id.aboutItemTextTitle, rows) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, null, parent)
        val row = rows[position]
        view.findViewById<TextView>(R.id.aboutItemTextTitle).text = row.title
        view.findViewById<TextView>(R.id.aboutItemTextSubtitle).text = row.subtitle
        row.iconId?.let {
            val drawable = ContextCompat.getDrawable(context, it)
            view.findViewById<ImageView>(R.id.aboutItemIcon)?.setImageDrawable(drawable)
        }
        return view
    }
}