package com.example.cafeenlinea.ui.onboarding.data

import android.content.Context
import com.example.cafeenlinea.common.preferences.AppPreferences

private const val KEY_COMPLETED_ONBOARDING = "completed_onboarding"


/* Marcado de onbording, para saber si ya esta completado o no, si no lo esta lo muestra */
class OnboardingPreferences(context: Context) {

    private val appPreferences = AppPreferences(context)

    fun hasCompletedOnboarding(): Boolean =
        appPreferences.getBoolean(KEY_COMPLETED_ONBOARDING)

    fun setOnboardingCompleted() {
        appPreferences.putBoolean(KEY_COMPLETED_ONBOARDING, true)
    }
}