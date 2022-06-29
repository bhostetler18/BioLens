package com.uf.automoth.ui.imaging

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

enum class AutoStopType {
    TIME, IMAGE_COUNT
}

@Serializable
data class ImagingSettings(
    var interval: Int = 20,
    var autoStop: Boolean = false,
    var autoStopType: AutoStopType = AutoStopType.IMAGE_COUNT,
    var autoStopValue: Int = 100
) {

    fun saveToFile(context: Context) {
        val encoded = JSON.encodeToString(this)
        context.openFileOutput(PATH, Context.MODE_PRIVATE).use {
            it.write(encoded.encodeToByteArray())
        }
    }

    companion object {
        const val PATH: String = "imaging_settings.json"
        private var JSON = Json { encodeDefaults = true }

        fun loadFromFile(context: Context): ImagingSettings? {
            return try {
                context.openFileInput(PATH)?.use { input ->
                    input.bufferedReader().use {
                        return Json.decodeFromString(it.readText())
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
