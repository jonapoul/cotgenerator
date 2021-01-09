package com.jonapoul.common.utils

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import java.lang.IllegalArgumentException

fun NavController.safelyNavigate(directions: NavDirections) {
    try {
        navigate(directions)
    } catch (e: IllegalArgumentException) {
        /* Thrown if the user double taps on the menu item before the app has a chance to remove the button.
         * No-op, we just put this here to avoid app crashing */
    }
}

/* See https://medium.com/androiddevelopers/sealed-with-a-class-a906f28ab7b5 */
val <T> T.exhaustive: T
    get() = this
