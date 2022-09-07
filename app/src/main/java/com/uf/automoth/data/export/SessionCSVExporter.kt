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
