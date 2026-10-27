package com.deedeedev.ytreader.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import com.deedeedev.ytreader.data.UserPreferencesRepository

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun applyReaderBrightness(activity: Activity?, brightness: Float) {
    val window = activity?.window ?: return
    val params = window.attributes
    params.screenBrightness = brightness.coerceIn(MIN_READER_BRIGHTNESS, 1f)
    window.attributes = params
}

internal fun currentEffectiveBrightness(
    activity: Activity?,
    appBrightnessPreference: Float
): Float {
    if (appBrightnessPreference != UserPreferencesRepository.BRIGHTNESS_FOLLOW_SYSTEM) {
        return appBrightnessPreference.coerceIn(MIN_READER_BRIGHTNESS, 1f)
    }
    val windowBrightness = activity?.window?.attributes?.screenBrightness
        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    return if (windowBrightness >= 0f) {
        windowBrightness.coerceIn(MIN_READER_BRIGHTNESS, 1f)
    } else {
        DEFAULT_GESTURE_BRIGHTNESS
    }
}
