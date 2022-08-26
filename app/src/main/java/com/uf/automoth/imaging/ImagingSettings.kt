package com.uf.automoth.imaging

import android.content.Context
import android.os.Parcelable
import androidx.preference.PreferenceManager
import com.uf.automoth.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

enum class AutoStopMode {
    OFF, TIME, IMAGE_COUNT
}

@Serializable
@Parcelize
data class ImagingSettings(
    var interval: Int = 60,
    var autoStopMode: AutoStopMode = AutoStopMode.TIME,
    var autoStopValue: Int = 720,
    var shutdownCameraWhenPossible: Boolean = false
) : Parcelable {

    fun saveToFile(context: Context) {
        val encoded = JSON.encodeToString(this)
        context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use {
            it.write(encoded.encodeToByteArray())
        }
    }

    companion object {
        const val FILENAME: String = "imaging_settings.json"
        private var JSON = Json { encodeDefaults = true }

        fun loadDefaults(context: Context): ImagingSettings? {
            return try {
                context.openFileInput(FILENAME)?.use { input ->
                    input.bufferedReader().use {
                        val settings: ImagingSettings = Json.decodeFromString(it.readText())
                        settings.shutdownCameraWhenPossible =
                            PreferenceManager.getDefaultSharedPreferences(context)
                                .getBoolean(context.getString(R.string.PREF_SHUTDOWN_CAMERA), false)
                        return settings
                    }
                }
            } catch (e: IOException) {
                return null
            } catch (e: SerializationException) {
                return null
            }
        }
    }
}
