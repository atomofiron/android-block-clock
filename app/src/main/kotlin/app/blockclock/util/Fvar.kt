package app.blockclock.util

import app.blockclock.model.FontAxis
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel.MapMode

private const val TTC_TAG = 0x74746366 // 'ttcf'
private const val FVAR_TAG = 0x66766172 // 'fvar'

/** The tag, the version and the amount of the faces in the collection header. */
private const val TTC_HEADER_SIZE = 12

/** The amount, the search hints and the table records of the offset table. */
private const val OFFSET_TABLE_HEADER_SIZE = 12
private const val TABLE_RECORD_SIZE = 16

/** The tag, the min, the default, the max, the flags and the name id of an axis. */
private const val AXIS_TAG_SIZE = 4
private const val AXIS_SIZE = 20
private const val FIXED_FACTOR = 65536f

/**
 * The axes declared in the `fvar` table of the face [ttcIndex] of the [file]:
 * the tag and the min/default/max of each axis. Empty for a static font or
 * for a file with a missing or a malformed table.
 */
fun readAxes(file: File, ttcIndex: Int): List<FontAxis> = try {
    FileInputStream(file).channel.use { channel ->
        val font = channel.map(MapMode.READ_ONLY, 0, channel.size()).order(ByteOrder.BIG_ENDIAN)
        font.axes(font.faceOffset(ttcIndex))
    }
} catch (_: Exception) {
    emptyList()
}

/** The offset of the table directory: the face offset of a collection file. */
private fun ByteBuffer.faceOffset(ttcIndex: Int): Int = when (getInt(0)) {
    TTC_TAG -> getInt(TTC_HEADER_SIZE + ttcIndex * Int.SIZE_BYTES)
    else -> 0
}

/** The offset of the [tag] table of the face, or -1 when the face has no such table. */
private fun ByteBuffer.tableOffset(faceOffset: Int, tag: Int): Int {
    val count = getShort(faceOffset + 4).toInt() and 0xFFFF
    for (index in 0 until count) {
        val record = faceOffset + OFFSET_TABLE_HEADER_SIZE + index * TABLE_RECORD_SIZE
        when (getInt(record)) {
            tag -> return getInt(record + 8)
        }
    }
    return -1
}

private fun ByteBuffer.axes(faceOffset: Int): List<FontAxis> {
    val fvar = tableOffset(faceOffset, FVAR_TAG)
    if (fvar < 0) {
        return emptyList()
    }
    val offset = fvar + (getShort(fvar + 4).toInt() and 0xFFFF)
    val count = getShort(fvar + 8).toInt() and 0xFFFF
    val size = (getShort(fvar + 10).toInt() and 0xFFFF).coerceAtLeast(AXIS_SIZE)
    if (offset + count.toLong() * size > capacity()) {
        return emptyList()
    }
    return List(count) { index ->
        val axis = offset + index * size
        FontAxis(
            tag = tag(axis),
            min = fixed(axis + AXIS_TAG_SIZE),
            default = fixed(axis + AXIS_TAG_SIZE + Int.SIZE_BYTES),
            max = fixed(axis + AXIS_TAG_SIZE + 2 * Int.SIZE_BYTES),
        )
    }
}

private fun ByteBuffer.tag(offset: Int): String {
    val bytes = ByteArray(AXIS_TAG_SIZE)
    for (index in bytes.indices) {
        bytes[index] = get(offset + index)
    }
    return String(bytes, Charsets.US_ASCII)
}

/** The 16.16 fixed point value of the font tables. */
private fun ByteBuffer.fixed(offset: Int): Float = getInt(offset) / FIXED_FACTOR
