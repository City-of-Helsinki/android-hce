package fi.hel.helsinkihcedemo

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

val TAG = "HelsinkiAPDU"

fun intToUnsignedByte(value: Int): Byte {
    return (value and 0xFF).toByte()
}

fun statusResponse(sw1: Apdu.Status, sw2: Int = 0): ByteArray {
    return byteArrayOf(sw1.toByte(), intToUnsignedByte(sw2))
}

fun setBody(data: ByteArray, result: ByteArray): Int {
    var i = 0
    while (i < data.size) {
        result[i] = data[i]
        i++
    }
    return i
}

fun dataResponse(data: ByteArray, status: Apdu.Status, sw2: Int = 0): ByteArray {
    val result = ByteArray(data.size + 2)
    val st = statusResponse(status, sw2)
    var i = setBody(data, result)
    while (i < result.size) {
        result[i] = st[i - data.size]
        i++
    }
    return result
}

fun chainedDataResponse(data: ByteBuffer?, limit: Int): ByteArray {
    if (data == null) {
        return ByteArray(0)
    }
    val numBytes = minOf(limit, data.remaining())
    var result = ByteArray(numBytes + 2)
    data.get(result, 0, numBytes)
    val remaining = data.remaining()
    if (remaining > 0) {
        result[numBytes] = Apdu.Status.CHAINED_RESPONSE.toByte()
        result[numBytes+1] = intToUnsignedByte(remaining)
    }
    else {
        result[numBytes] = Apdu.Status.SUCCESS.toByte()
        result[numBytes+1] = 0
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
        val MIN_APDU_LENGTH = 5
    }
    enum class MessageType {
        COMMAND, RESPONSE
    }
    enum class Status(val value: Int) {
        SUCCESS(0x90),
        ERROR(0x6f),
        INSTRUCTION_NOT_SUPPORTED(0x6d),
        CHAINED_RESPONSE(0x61),
        CLASS_NOT_SUPPORTED(0x6e);
        fun toByte(): Byte {
            return (this.value and 0xFF).toByte()
        }
    }
    enum class Instruction(val value: Int) {
        SELECT(0xa4),
        EXTERNAL_AUTHENTICATE(0x82),
        GET_RESPONSE(0xc0),
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

    fun getUnsignedByte(buffer: ByteBuffer): Int {
        return 0x000000FF and buffer.get().toInt()
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
        this.cls = getUnsignedByte(buffer)
        this.instruction = Instruction.getMatching(getUnsignedByte(buffer))
        this.parameter1 = getUnsignedByte(buffer)
        this.parameter2 = getUnsignedByte(buffer)
        if (this.instruction == Instruction.GET_RESPONSE) {
            this.expectedLength = getUnsignedByte(buffer)
            this.contentLength = null
            this.data = null
        }
        else {
            this.contentLength = getUnsignedByte(buffer)
            this.data = ByteArray(this.contentLength)
            buffer.get(this.data)
            if (buffer.hasRemaining()) {
                this.expectedLength = getUnsignedByte(buffer)
            }
            else {
                this.expectedLength = null
            }
        }
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
