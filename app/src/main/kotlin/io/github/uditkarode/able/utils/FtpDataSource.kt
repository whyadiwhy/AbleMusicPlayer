package io.github.uditkarode.able.utils

import android.media.MediaDataSource
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import java.io.InputStream

@RequiresApi(Build.VERSION_CODES.M)
class FtpDataSource(private var `is`: InputStream, streamLength: Long, private var ftpFile: String?):
    MediaDataSource() {
    private var streamLength: Long = -1
    private var lastReadEndPosition: Long = 0

    @Synchronized
    @Throws(IOException::class)
    override fun close() {
        `is`.close()
    }

    @Synchronized
    @Throws(IOException::class)
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, s: Int): Int {
        var size = s
        if (position >= streamLength) return -1
        if (position + size > streamLength) size -= (position + size - streamLength.toInt()).toInt()
        if (position < lastReadEndPosition) {
            `is`.close()
            lastReadEndPosition = 0
            `is` = Shared.fc.retrieveFileStream(ftpFile)
        }
        val skipped = `is`.skip(position - lastReadEndPosition)
        return if (skipped == position - lastReadEndPosition) {
            val bytesRead = `is`.read(buffer, offset, size)
            lastReadEndPosition = position + bytesRead
            bytesRead
        } else {
            -1
        }
    }

    @Synchronized
    @Throws(IOException::class)
    override fun getSize(): Long {
        return streamLength
    }

    init {
        this.streamLength = streamLength
        if (streamLength <= 0) {
            try {
                this.streamLength = `is`.available()
                    .toLong() //Correct value of InputStream#available() method not always supported by InputStream implementation!
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}