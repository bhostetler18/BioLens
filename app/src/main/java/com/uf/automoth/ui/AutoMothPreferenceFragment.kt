package com.uf.automoth.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.uf.automoth.R

class AutoMothPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey)
    }
}
