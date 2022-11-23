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

package com.uf.automoth.data.export

import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.utility.CSVWriter
import java.io.File

class SessionCSVExporter(
    private val formatter: SessionCSVFormatter
) {
    suspend fun export(session: Session, file: File) {
        CSVWriter(file).use { writer ->
            writer.writeLine(formatter.getHeader())
            for (image in AutoMothRepository.getImagesInSession(session.sessionID)) {
                writer.writeLine(formatter.getRow(image))
            }
        }
    }
}
