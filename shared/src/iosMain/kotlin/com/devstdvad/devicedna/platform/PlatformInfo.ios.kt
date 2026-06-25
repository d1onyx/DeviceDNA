package com.devstdvad.devicedna.platform

import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

actual object PlatformInfo {
    actual val deviceModel: String
        get() = UIDevice.currentDevice.model
    actual val manufacturer: String
        get() = "Apple"
    actual val osName: String
        get() = UIDevice.currentDevice.systemName
    actual val osVersion: String
        get() = UIDevice.currentDevice.systemVersion
    actual val processorCount: Int
        get() = NSProcessInfo.processInfo.processorCount.toInt()
    actual val totalMemoryBytes: Long
        get() = NSProcessInfo.processInfo.physicalMemory.toLong()
}
