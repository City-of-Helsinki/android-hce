package fi.hel.helsinkihcedemo

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.nio.ByteBuffer

import fi.hel.helsinkihcedemo.statusResponse
import fi.hel.helsinkihcedemo.dataResponse
import fi.hel.helsinkihcedemo.defaultErrorResponse

/**
 * Created by tituomin on 6.2.2018.
 */
class HostCardEmulatorService: HostApduService() {
    var applicationSelected = false
    var clientIdVerified = false
    var sendingData = false
    var dataBuffer: ByteBuffer? = null
    companion object {
        val TAG = "HelsinkiHostCardEmulator"
        val AID = Utils.hexStringToByteArray("F074756E6E697374616D6F")
        val CARD_READER_CLIENT_ID = ByteArray(50, {i -> 0x61})
        val RESPONSE_DATA = ByteArray(1024, {i -> (i % 100).toByte()})
        val CHUNK_SIZE = 255
    }
    override fun onDeactivated(reason: Int) {
        resetState()
        Log.d(TAG, "Deactivated: " + reason)
    }
    fun resetState() {
        applicationSelected = false
        clientIdVerified = false
        sendingData = false
    }
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            return defaultErrorResponse()
        }

        val apdu = Apdu(commandApdu)
        Log.d(TAG, apdu.toString())

        val errorStatus = apdu.errorStatus()
        if (errorStatus != null) {
            return statusResponse(errorStatus)
        }
        if (apdu.instruction == Apdu.Instruction.SELECT) {
            if (apdu.data != null && apdu.data contentEquals AID) {
                applicationSelected = true
                return statusResponse(Apdu.Status.SUCCESS)
            }
        }
        if (apdu.instruction == Apdu.Instruction.EXTERNAL_AUTHENTICATE &&
            apdu.parameter1 == 1 && apdu.parameter2 == 1) {
            if (apdu.data != null && apdu.data contentEquals CARD_READER_CLIENT_ID) {
                clientIdVerified = true;
            }
        }
        if (apdu.instruction == Apdu.Instruction.INTERNAL_AUTHENTICATE &&
            apdu.parameter1 == 1 && apdu.parameter2 == 1 &&
            applicationSelected && clientIdVerified) {
            dataBuffer = ByteBuffer.wrap(RESPONSE_DATA)
            return chainedDataResponse(dataBuffer, CHUNK_SIZE)
        }
        if (apdu.instruction == Apdu.Instruction.GET_RESPONSE) {
            return chainedDataResponse(dataBuffer, CHUNK_SIZE)
        }
        return defaultErrorResponse()
    }
}
