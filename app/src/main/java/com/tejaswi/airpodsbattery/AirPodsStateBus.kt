package com.tejaswi.airpodsbattery

import com.tejaswi.airpodsbattery.model.AirPodsBattery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single live state source for the app, service, and widget.
 * Battery values update immediately; noisy charging/ear flags require two
 * consecutive matching advertisements before they change.
 */
object AirPodsStateBus {
    private val _battery = MutableStateFlow<AirPodsBattery?>(null)
    val battery: StateFlow<AirPodsBattery?> = _battery.asStateFlow()

    private var stable: AirPodsBattery? = null
    private var chargingCandidate: Triple<Boolean, Boolean, Boolean>? = null
    private var chargingCandidateCount = 0
    private var earCandidate: Pair<Boolean?, Boolean?>? = null
    private var earCandidateCount = 0

    @Synchronized
    fun publish(incoming: AirPodsBattery): AirPodsBattery {
        val old = stable
        if (old == null) {
            stable = incoming
            _battery.value = incoming
            return incoming
        }

        val incomingCharging = Triple(incoming.leftCharging, incoming.rightCharging, incoming.caseCharging)
        val oldCharging = Triple(old.leftCharging, old.rightCharging, old.caseCharging)
        if (incomingCharging == oldCharging) {
            chargingCandidate = null
            chargingCandidateCount = 0
        } else {
            if (chargingCandidate == incomingCharging) chargingCandidateCount++
            else {
                chargingCandidate = incomingCharging
                chargingCandidateCount = 1
            }
        }
        val charging = if (chargingCandidateCount >= 2) incomingCharging else oldCharging
        if (chargingCandidateCount >= 2) {
            chargingCandidate = null
            chargingCandidateCount = 0
        }

        val incomingEar = Pair(incoming.leftInEar, incoming.rightInEar)
        val oldEar = Pair(old.leftInEar, old.rightInEar)
        if (incomingEar == oldEar || incomingEar.first == null || incomingEar.second == null) {
            if (incomingEar == oldEar) { earCandidate = null; earCandidateCount = 0 }
        } else {
            if (earCandidate == incomingEar) earCandidateCount++
            else {
                earCandidate = incomingEar
                earCandidateCount = 1
            }
        }
        val ear = if (earCandidateCount >= 2) incomingEar else oldEar
        if (earCandidateCount >= 2) {
            earCandidate = null
            earCandidateCount = 0
        }

        val merged = incoming.copy(
            left = incoming.left ?: old.left,
            right = incoming.right ?: old.right,
            case = incoming.case ?: old.case,
            leftCharging = charging.first,
            rightCharging = charging.second,
            caseCharging = charging.third,
            leftInEar = ear.first,
            rightInEar = ear.second,
            deviceName = if (incoming.deviceName.isBlank()) old.deviceName else incoming.deviceName
        )
        stable = merged
        _battery.value = merged
        return merged
    }
}
