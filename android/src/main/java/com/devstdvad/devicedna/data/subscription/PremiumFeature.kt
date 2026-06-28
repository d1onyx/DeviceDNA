package com.devstdvad.devicedna.data.subscription

enum class PremiumFeature(val key: String) {
    RemoveAds("remove_ads"),
    Widgets("widgets"),
    BatteryIntelligence("battery_intelligence"),
    ;

    companion object {
        fun fromKey(key: String): PremiumFeature? = entries.firstOrNull { it.key == key }
    }
}
