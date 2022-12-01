/*
 * Copyright (c) 2022 University of Florida
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

package com.uf.biolens.data.metadata

import androidx.annotation.StringRes
import com.uf.biolens.R

private const val SHEET_WIDTH = "session_sheet_width_cm"
private const val SHEET_HEIGHT = "session_sheet_height_cm"
private const val HAS_SCALE_BAR = "session_has_scale_bar"
private const val HAS_COLOR_STANDARD = "session_has_color_standard"
private const val UV_LIGHT_MODEL = "session_UV_light_model"
private const val WHITE_LIGHT_MODEL = "session_white_light_model"
private const val HARDWARE_DESCRIPTION = "session_hardware_description"

data class BuiltinMetadata(
    val name: String,
    val type: MetadataType,
    @StringRes val translation: Int
)

val AUTOMOTH_METADATA = listOf(
    BuiltinMetadata(SHEET_WIDTH, MetadataType.DOUBLE, R.string.metadata_sheet_width_cm),
    BuiltinMetadata(SHEET_HEIGHT, MetadataType.DOUBLE, R.string.metadata_sheet_height_cm),
    BuiltinMetadata(HAS_SCALE_BAR, MetadataType.BOOLEAN, R.string.metadata_has_scale_bar),
    BuiltinMetadata(HAS_COLOR_STANDARD, MetadataType.BOOLEAN, R.string.metadata_has_color_standard),
    BuiltinMetadata(UV_LIGHT_MODEL, MetadataType.STRING, R.string.metadata_uv_light_model),
    BuiltinMetadata(WHITE_LIGHT_MODEL, MetadataType.STRING, R.string.metadata_white_light_model),
    BuiltinMetadata(
        HARDWARE_DESCRIPTION,
        MetadataType.STRING,
        R.string.metadata_hardware_description
    )
)

private fun greaterThanZero(value: Any?): Boolean {
    return value == null || ((value as? Double) ?: 0.0) > 0.0
}

val AUTOMOTH_METADATA_VALIDATORS: Map<String, (Any?) -> Boolean> = mapOf(
    SHEET_WIDTH to ::greaterThanZero,
    SHEET_HEIGHT to ::greaterThanZero
)

suspend fun migrate(store: MetadataStore) {
    store.renameField("sheet_width", SHEET_WIDTH, true)
    store.renameField("sheet_height", SHEET_HEIGHT, true)
}

suspend fun prepopulate(store: MetadataStore) {
    migrate(store)
    AUTOMOTH_METADATA.forEach {
        if (store.getField(it.name) == null) {
            store.addMetadataField(it.name, it.type, true)
        }
    }
}
