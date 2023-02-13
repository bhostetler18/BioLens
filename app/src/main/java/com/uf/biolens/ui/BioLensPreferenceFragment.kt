/*
 * Copyright (c) 2022-2023 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.uf.biolens.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.biolens.BuildConfig
import com.uf.biolens.R
import com.uf.biolens.network.GoogleSignInHelper
import com.uf.biolens.ui.common.simpleAlertDialogWithOk

class BioLensPreferenceFragment : PreferenceFragmentCompat() {

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder()
            .requestScopes(GoogleSignInHelper.DRIVE_FILE_SCOPE)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(requireContext(), options)
    }
    private val signInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .addOnSuccessListener { account: GoogleSignInAccount ->
                        setGoogleAccount(account)
                    }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey)
        setGoogleAccount(GoogleSignInHelper.getGoogleAccountIfValid(requireContext()))
        showAppVersion()
    }

    private fun showAppVersion() {
        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        val pref =
            preferenceScreen.findPreference<Preference>(getString(R.string.PREF_APP_VERSION))
                ?: return
        pref.title = getString(R.string.app_version, versionName)
        pref.summary = getString(R.string.app_build, versionCode)
    }

    private fun signIn() {
        val googleAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleAvailability.isUserResolvableError(resultCode)) {
                googleAvailability.getErrorDialog(this, resultCode, 1)?.show()
            } else {
                simpleAlertDialogWithOk(
                    requireContext(),
                    R.string.cannot_sign_in,
                    R.string.warn_no_google_services
                ).show()
            }
            return
        }
        val account = GoogleSignInHelper.getGoogleAccountIfValid(requireContext())
        if (account == null) {
            signInLauncher.launch(signInClient.signInIntent)
        } else {
            setGoogleAccount(account)
        }
    }

    private fun signOut() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.warn_sign_out)
            .setPositiveButton(R.string.sign_out) { dialog, _ ->
                signInClient.signOut().addOnSuccessListener {
                    setGoogleAccount(null)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun setGoogleAccount(account: GoogleSignInAccount?) {
        val pref =
            preferenceScreen.findPreference<Preference>(getString(R.string.PREF_GOOGLE_ACCOUNT))
                ?: return
        if (account != null && account.email != null) {
            pref.title = getString(R.string.signed_in_as_email, account.email)
            pref.summary = getString(R.string.sign_out)
            pref.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_logout_24, null)
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                signOut()
                true
            }
        } else {
            pref.title = getString(R.string.no_google_account)
            pref.summary = getString(R.string.sign_in)
            pref.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_login_24, null)
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                signIn()
                true
            }
        }
    }
}
