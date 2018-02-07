package fi.hel.helsinkihcedemo

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

/**
 * Created by tituomin on 6.2.2018.
 */
class HostCardEmulatorService: HostApduService() {
    companion object {
        val TAG = "HelsinkiHostCardEmulator"
        val STATUS_SUCCESS = "9000"
        val STATUS_FAILED = "6F00"
        val CLA_NOT_SUPPORTED = "6E00"
        val INS_NOT_SUPPORTED = "6D00"
        val AID = "F074756E6E697374616D6F"
        val SELECT_INS = "A4"
        val DEFAULT_CLA = "00"
        val MIN_APDU_LENGTH = 12
        val TEST_REQUEST = "008801010000"
    }
    // TODO: raportoi: ei NFC:tä
    // TODO: javascript-kutsu
    // TODO: 

    // APDU: CLA  INS  P1  P2 
    // select: 
    // 00A404000BF074756E6E697374616D6F00
    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: " + reason)
    }
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            Log.d(TAG, "iz null")
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }
        val hexCommandApdu = Utils.toHex(commandApdu)
        Log.d(TAG, hexCommandApdu)
        return Utils.hexStringToByteArray(when {
            hexCommandApdu == TEST_REQUEST -> ("A".repeat(2048) + STATUS_SUCCESS)
            hexCommandApdu.length < MIN_APDU_LENGTH -> STATUS_FAILED
            hexCommandApdu.substring(0, 2) != DEFAULT_CLA -> CLA_NOT_SUPPORTED
            hexCommandApdu.substring(2, 4) != SELECT_INS -> INS_NOT_SUPPORTED
            hexCommandApdu.substring(10, 32) == AID -> STATUS_SUCCESS
            else -> STATUS_FAILED
        })
    }
}
