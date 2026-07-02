package com.devstdvad.devicedna.data.sync

import java.security.MessageDigest

actual fun sha256Hex(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.encodeToByteArray())
        .joinToString("") { byte -> ((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1) }
