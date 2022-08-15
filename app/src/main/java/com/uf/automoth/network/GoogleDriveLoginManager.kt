package com.uf.automoth.network

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.util.*

class GoogleDriveLoginManager(private val activity: GoogleDriveSignInActivity) {

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder()
            .requestScopes(DRIVE_FILE_SCOPE)
            .requestEmail().build()
        GoogleSignIn.getClient(activity.appContext, options)
    }
    var currentAccount: GoogleSignInAccount? = null
    var driveService: Drive? = null

    fun signInIfNecessary() {
        val account = GoogleSignIn.getLastSignedInAccount(activity.appContext)
        if (account == null) {
            signIn(activity)
        } else {
            currentAccount = account
            setupDriveClient(account)
        }
    }

    private fun signIn(activity: GoogleDriveSignInActivity) {
        activity.signInLauncher.launch(signInClient.signInIntent)
    }

    fun signOut() {
        signInClient.signOut()
        currentAccount = null
    }

    fun handleSignInResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .addOnSuccessListener { account: GoogleSignInAccount ->
                    currentAccount = account
                    Log.d(TAG, "Signed in as " + account.email)
                    setupDriveClient(account)
                }.addOnFailureListener { exception: Exception? ->
                    Log.e(
                        TAG,
                        "Unable to sign in.",
                        exception
                    )
                }
        }
    }

    private fun setupDriveClient(account: GoogleSignInAccount) {
        val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
            activity.appContext,
            Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(activity.applicationName).build()
    }

    companion object {
        private const val TAG = "[GOOGLE DRIVE]"
        val DRIVE_FILE_SCOPE = Scope(DriveScopes.DRIVE_FILE)
    }
}
