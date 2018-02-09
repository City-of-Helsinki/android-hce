package fi.hel.helsinkihcedemo

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

val TAG = "HelsinkiAPDU"

fun statusResponse(error: Apdu.Status): ByteArray {
    return byteArrayOf(error.toByte(), 0)
}

fun successResponse(data: ByteArray): ByteArray {
    val result = ByteArray(data.size + 2)
    val status = statusResponse(Apdu.Status.SUCCESS)
    var i = 0
    while (i < data.size) {
        result[i] = data[i]
        i++
    }
    while (i < result.size) {
        result[i] = status[i - data.size]
        i++
    }
    return result
}

fun defaultErrorResponse(): ByteArray {
    return statusResponse(Apdu.Status.ERROR)
}

class Apdu {
    companion object {
        val AID = Utils.hexStringToByteArray("F074756E6E697374616D6F")
        val DEFAULT_CLA = 0x00
        val MIN_APDU_LENGTH = 6
    }
    enum class MessageType {
        COMMAND, RESPONSE
    }
    enum class Status(val value: Int) {
        SUCCESS(0x90),
        ERROR(0x6f),
        INSTRUCTION_NOT_SUPPORTED(0x6d),
        CLASS_NOT_SUPPORTED(0x6e);
        fun toByte(): Byte {
            return (this.value and 0xFF).toByte()
        }
    }
    enum class Instruction(val value: Int) {
        SELECT(0xa4),
        INTERNAL_AUTHENTICATE(0x88);
        companion object {
             fun getMatching(value: Int): Instruction? {
                 for (ins in Instruction.values()) {
                     if (ins.value == value) {
                         return ins
                     }
                 }
                 return null
             }
         }
    }

    val messageType: MessageType?
    val instruction: Instruction?
    val cls: Int?
    val parameter1: Int?
    val parameter2: Int?
    val contentLength: Int?
    val expectedLength: Int?
    val data: ByteArray?

    val status: Status?

    fun getUnsignedShort(buffer: ByteBuffer): Int {
        return 0x000000FF and buffer.get().toInt()
    }

    fun intToUnsignedByte(value: Int): Byte {
        return (value and 0xFF).toByte()
    }

    constructor (commandApdu: ByteArray) {
        this.messageType = MessageType.COMMAND
        if (commandApdu.size < MIN_APDU_LENGTH) {
            status = Status.ERROR
            instruction = null
            cls = null
            parameter1 = null
            parameter2 = null
            contentLength = null
            expectedLength = null
            data = null
            return
        }

        val buffer: ByteBuffer = ByteBuffer.wrap(commandApdu)
        this.cls = getUnsignedShort(buffer)
        this.instruction = Instruction.getMatching(getUnsignedShort(buffer))
        this.parameter1 = getUnsignedShort(buffer)
        this.parameter2 = getUnsignedShort(buffer)
        this.contentLength = getUnsignedShort(buffer)
        this.data = ByteArray(this.contentLength)
        buffer.get(this.data)
        this.expectedLength = getUnsignedShort(buffer)
        this.status = null
    }

    fun errorStatus(): Status? {
        if (messageType == MessageType.COMMAND) {
            if (instruction == null) {
                return Status.INSTRUCTION_NOT_SUPPORTED
            }
            if (cls != DEFAULT_CLA) {
                return Status.CLASS_NOT_SUPPORTED
            }
        }
        return null
    }

    override fun toString(): String {
        val hexString: String
        if (data == null) {
            hexString = ""
        } else {
            hexString = Utils.toHex(data)
        }

        return (
                """type: $messageType
                |class: $cls
                |instruction: $instruction
                |parameter 1: $parameter1
                |parameter 2: $parameter2
                |content length: $contentLength
                |data: $hexString
                """.trimMargin())
    }

}
