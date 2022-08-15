package com.uf.automoth.network

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

interface GoogleDriveSignInActivity {
    val appContext: Context
    val signInLauncher: ActivityResultLauncher<Intent>
    val applicationName: String
}
