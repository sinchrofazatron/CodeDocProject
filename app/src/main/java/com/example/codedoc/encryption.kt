package com.example.codedoc

import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher

class encryption {
    fun generateKeyPair(): Pair<PublicKey, PrivateKey>{
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()
        return Pair(keyPair.public, keyPair.private)
    }

    fun encrypt(text: String, publicKey: PublicKey): ByteArray{
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(text.toByteArray())
    }

    fun decrypt(encryptedText: ByteArray, privateKey: PrivateKey): String{
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return String(cipher.doFinal(encryptedText))
    }

}