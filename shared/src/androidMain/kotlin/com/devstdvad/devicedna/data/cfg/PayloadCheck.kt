package com.devstdvad.devicedna.data.cfg

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

class PayloadCheck(publicKey: ByteArray) : SignatureCheck {
    private val params = Ed25519PublicKeyParameters(publicKey, 0)

    override fun verify(message: ByteArray, signature: ByteArray): Boolean {
        val signer = Ed25519Signer()
        signer.init(false, params)
        signer.update(message, 0, message.size)
        return signer.verifySignature(signature)
    }
}
