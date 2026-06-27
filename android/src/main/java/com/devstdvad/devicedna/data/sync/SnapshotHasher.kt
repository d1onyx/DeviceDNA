package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.domain.model.DeviceSnapshot
import java.security.MessageDigest

/**
 * Stable snapshot hash built only from identity/configuration fields that do NOT change
 * every moment. This way a re-push happens only on a real device change, not because of
 * fluctuating load/temperature/frequencies.
 */
object SnapshotHasher {

    fun stableHash(s: DeviceSnapshot): String {
        val sb = StringBuilder()
        s.device?.let {
            sb.append(it.androidId).append('|')
                .append(it.buildFingerprint).append('|')
                .append(it.manufacturer).append('|')
                .append(it.model).append('|')
                .append(it.socName).append('|')
                .append(it.isRooted)
        }
        s.system?.let {
            sb.append("|sys|").append(it.androidVersion).append('|')
                .append(it.apiLevel).append('|')
                .append(it.buildNumber).append('|')
                .append(it.securityPatchLevel).append('|')
                .append(it.appVersionCode)
        }
        s.display?.let { sb.append("|disp|").append(it.widthPx).append('x').append(it.heightPx) }
        s.storage?.let { sb.append("|st|").append(it.totalBytes) }
        s.ram?.let { sb.append("|ram|").append(it.totalBytes) }
        s.cpu?.let { sb.append("|cpu|").append(it.chipsetName).append('|').append(it.coreCount) }
        s.camera?.let { sb.append("|cam|").append(it.cameras.size) }
        s.sensors?.let { sb.append("|sn|").append(it.sensors.size) }
        s.apps?.let { sb.append("|app|").append(it.totalCount) }
        return sha256(sb.toString())
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
