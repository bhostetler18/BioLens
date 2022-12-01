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

package com.uf.biolens.utility

import java.io.Closeable
import java.io.File
import java.io.PrintWriter

class CSVWriter(
    file: File,
    private val separator: String = ","
) : Closeable {
    private val writer: PrintWriter = PrintWriter(file)

    fun writeLine(values: List<String>) {
        val row = values.joinToString(separator) { sanitize(it) }
        writer.write(row)
        writer.println()
    }

    private fun sanitize(string: String): String {
        // If desired, we can escape these instead in the future
        return string.replace("\n", "").replace(separator, "")
    }

    override fun close() {
        writer.close()
    }
}
