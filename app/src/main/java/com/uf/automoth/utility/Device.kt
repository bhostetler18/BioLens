package com.uf.automoth.utility

import android.os.Build

fun getDeviceType(): String {
    return "${Build.MANUFACTURER}: ${Build.MODEL}"
}
