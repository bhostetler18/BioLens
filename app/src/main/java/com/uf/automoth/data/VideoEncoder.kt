package com.uf.automoth.data

import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jcodec.api.SequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Rational
import org.jcodec.scale.BitmapUtil
import java.io.File
import java.io.IOException

class VideoEncoder {

    suspend fun encode(session: Session) {
        withContext(Dispatchers.IO) {
            try {
                val allImages = AutoMothRepository.getImagesInSessionBlocking(session.sessionID)
                val output = File(AutoMothRepository.storageLocation, "${session.name}.mp4")
                val enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), Rational.ONE)
                for (image in allImages) {
                    val file = AutoMothRepository.resolve(image, session)
                    val bitmap = BitmapFactory.decodeFile(file.path)
                    val picture = BitmapUtil.fromBitmap(bitmap)
                    enc.encodeNativeFrame(picture)
                }
                enc.finish()
            } catch (e: IOException) {
                Log.d(TAG, e.localizedMessage)
            }
        }
    }

    companion object {
        const val TAG = "[VIDEO_ENCODER]"
    }
}
