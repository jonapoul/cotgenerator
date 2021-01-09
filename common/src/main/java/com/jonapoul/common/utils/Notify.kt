package com.jonapoul.common.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

object Notify {
    private const val DEFAULT_TEXT = "OK"
    private val DEFAULT_ACTION = View.OnClickListener { /* do nothing */ }

    private var anchor: View? = null

    fun setAnchor(view: View?) {
        this.anchor = view
    }

    private fun snackbar(
            root: View,
            message: String,
            backgroundColour: Int,
            textColour: Int,
            action: View.OnClickListener?,
            actionMsg: String?
    ) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(backgroundColour)
        val text = snackbar.view.findViewById<TextView>(R.id.snackbar_text)
        text.setTextColor(textColour)
        text.maxLines = 20 // don't crop off any longer messages

        snackbar.anchorView = anchor

        /* Add any onclick actions */
        if (action != null && actionMsg != null) {
            snackbar.setAction(actionMsg, action)
            snackbar.setActionTextColor(textColour)
        }
        snackbar.show()
    }

    fun red(
            root: View,
            message: String,
            action: View.OnClickListener = DEFAULT_ACTION,
            actionMsg: String = DEFAULT_TEXT
    ) {
        Timber.e(message)
        snackbar(root, message, Color.RED, Color.BLACK, action, actionMsg)
    }

    fun green(
            root: View,
            message: String,
            action: View.OnClickListener = DEFAULT_ACTION,
            actionMsg: String = DEFAULT_TEXT
    ) {
        Timber.i(message)
        snackbar(root, message, Color.GREEN, Color.BLACK, action, actionMsg)
    }

    fun orange(
            root: View,
            message: String,
            action: View.OnClickListener = DEFAULT_ACTION,
            actionMsg: String = DEFAULT_TEXT
    ) {
        Timber.w(message)
        val orange = Color.parseColor("#FFA600")
        snackbar(root, message, orange, Color.BLACK, action, actionMsg)
    }

    fun blue(
            root: View,
            message: String,
            action: View.OnClickListener = DEFAULT_ACTION,
            actionMsg: String = DEFAULT_TEXT
    ) {
        Timber.i(message)
        snackbar(root, message, Color.BLUE, Color.WHITE, action, actionMsg)
    }

    fun yellow(
            root: View,
            message: String,
            action: View.OnClickListener = DEFAULT_ACTION,
            actionMsg: String = DEFAULT_TEXT
    ) {
        Timber.i(message)
        snackbar(root, message, Color.YELLOW, Color.BLACK, action, actionMsg)
    }

    fun toast(context: Context, message: String) {
        Timber.i(message)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun alert(context: Context, message: String) {
        MaterialAlertDialogBuilder(context)
                .setTitle("Alert")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }
}