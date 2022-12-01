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

package com.uf.biolens.data.export

import com.uf.biolens.data.Image

interface SessionCSVFormatter {
    fun getHeader(): List<String>
    fun getRow(image: Image): List<String>
}

data class BasicSessionFormatterColumn(
    val name: String,
    val getter: (Image) -> String
)

abstract class BasicSessionCSVFormatter : SessionCSVFormatter {
    private var columns = ArrayList<BasicSessionFormatterColumn>()

    fun addColumn(name: String, getter: (Image) -> String) {
        columns.add(BasicSessionFormatterColumn(name, getter))
    }

    fun addConstantColumn(name: String, value: String) {
        columns.add(BasicSessionFormatterColumn(name) { value })
    }

    override fun getHeader(): List<String> {
        return columns.map {
            it.name
        }
    }

    override fun getRow(image: Image): List<String> {
        return columns.map {
            it.getter(image)
        }
    }
}
