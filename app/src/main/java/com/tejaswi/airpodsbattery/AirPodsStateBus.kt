package com.tejaswi.airpodsbattery

import com.tejaswi.airpodsbattery.model.AirPodsBattery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AirPodsStateBus {
    private val _battery = MutableStateFlow<AirPodsBattery?>(null)
    val battery: StateFlow<AirPodsBattery?> = _battery.asStateFlow()

    fun publish(battery: AirPodsBattery) {
        _battery.value = battery
    }
}
