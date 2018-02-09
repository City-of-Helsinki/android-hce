package fi.hel.helsinkihcedemo

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

import fi.hel.helsinkihcedemo.statusResponse
import fi.hel.helsinkihcedemo.successResponse
import fi.hel.helsinkihcedemo.defaultErrorResponse

/**
 * Created by tituomin on 6.2.2018.
 */
class HostCardEmulatorService: HostApduService() {
    companion object {
        val TAG = "HelsinkiHostCardEmulator"
        val AID = Utils.hexStringToByteArray("F074756E6E697374616D6F")
    }
    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: " + reason)
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
                return statusResponse(Apdu.Status.SUCCESS)
            }
        }
        if (apdu.instruction == Apdu.Instruction.INTERNAL_AUTHENTICATE &&
            apdu.parameter1 == 1 && apdu.parameter2 == 1) {
            return successResponse(ByteArray(1024, {i -> 100}))
        }
        return defaultErrorResponse()
    }
}
