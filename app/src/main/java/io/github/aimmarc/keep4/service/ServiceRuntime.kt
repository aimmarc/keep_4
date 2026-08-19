package io.github.aimmarc.keep4.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ServiceMode {
    STOPPED,
    MONITORING,
    RECORDING,
    ERROR,
}

data class ServiceSnapshot(
    val mode: ServiceMode = ServiceMode.STOPPED,
    val detail: String? = null,
)

object ServiceRuntime {
    private val mutableSnapshot = MutableStateFlow(ServiceSnapshot())
    val snapshot = mutableSnapshot.asStateFlow()

    private val mutableAccessibilityConnected = MutableStateFlow(false)
    val accessibilityConnected = mutableAccessibilityConnected.asStateFlow()

    fun update(mode: ServiceMode, detail: String? = null) {
        mutableSnapshot.value = ServiceSnapshot(mode, detail)
    }

    fun setAccessibilityConnected(connected: Boolean) {
        mutableAccessibilityConnected.value = connected
    }
}
