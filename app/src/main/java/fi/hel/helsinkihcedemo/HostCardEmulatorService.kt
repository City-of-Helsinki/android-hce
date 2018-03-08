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
    var interfaceDeviceId: ByteArray? = null
    var sendingData = false
    var dataBuffer: ByteBuffer? = null
    companion object {
        val TAG = "HelsinkiHostCardEmulator"
        val AID = Utils.hexStringToByteArray("F074756E6E697374616D6F")
        val RESPONSE_DATA = ByteArray(700, {i -> (0x41 + i % 58).toByte()})
    }
    fun logResponse(data: ByteArray): ByteArray {
        Log.d(TAG, "Responding with: " + Utils.toHex(data))
        return data
    }
    override fun onDeactivated(reason: Int) {
        resetState()
        Log.d(TAG, "Deactivated: " + reason)
    }
    fun resetState() {
        applicationSelected = false
        interfaceDeviceId = null
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
            return logResponse(statusResponse(errorStatus))
        }
        if (apdu.instruction == Apdu.Instruction.SELECT) {
            if (apdu.data != null && apdu.data contentEquals AID) {
                applicationSelected = true
                return logResponse(statusResponse(Apdu.Status.SUCCESS))
            }
        }
        if (apdu.instruction == Apdu.Instruction.EXTERNAL_AUTHENTICATE &&
            apdu.parameter1 == 1 && apdu.parameter2 == 1) {
            if (apdu.data != null) {
                interfaceDeviceId = apdu.data
                return logResponse(statusResponse(Apdu.Status.SUCCESS))
            }
        }
        if (apdu.instruction == Apdu.Instruction.INTERNAL_AUTHENTICATE &&
            apdu.parameter1 == 1 && apdu.parameter2 == 1 &&
            applicationSelected) {
            dataBuffer = ByteBuffer.wrap(RESPONSE_DATA)
            return logResponse(chainedDataResponse(dataBuffer, apdu.expectedLength))
        }
        if (apdu.instruction == Apdu.Instruction.GET_RESPONSE) {
            return logResponse(chainedDataResponse(dataBuffer, apdu.expectedLength))
        }
        return logResponse(defaultErrorResponse())
    }
}
