package com.uf.automoth.network

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

object GoogleSignInHelper {

    fun getGoogleAccountIfValid(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null &&
            account.email != null &&
            GoogleSignIn.hasPermissions(account, DRIVE_FILE_SCOPE)
        ) {
            account
        } else {
            null
        }
    }

    val DRIVE_FILE_SCOPE = Scope(DriveScopes.DRIVE_FILE)
}
