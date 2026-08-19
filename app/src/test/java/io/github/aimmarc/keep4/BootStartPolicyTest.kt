package io.github.aimmarc.keep4

import io.github.aimmarc.keep4.receiver.BootAction
import io.github.aimmarc.keep4.receiver.BootStartPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class BootStartPolicyTest {
    @Test
    fun disabledSettingIsIgnored() {
        assertEquals(BootAction.IGNORE, decide(enabled = false))
    }

    @Test
    fun missingMicrophonePermissionUsesRecoveryNotification() {
        assertEquals(
            BootAction.SHOW_RECOVERY_NOTIFICATION,
            decide(hasPermission = false),
        )
    }

    @Test
    fun android14UsesRecoveryNotification() {
        assertEquals(BootAction.SHOW_RECOVERY_NOTIFICATION, decide(sdkInt = 34))
    }

    @Test
    fun android13CanStartForegroundServiceDirectly() {
        assertEquals(BootAction.START_SERVICE, decide(sdkInt = 33))
    }

    private fun decide(
        enabled: Boolean = true,
        hasPermission: Boolean = true,
        sdkInt: Int = 33,
    ): BootAction {
        return BootStartPolicy.decide(
            enabled = enabled,
            startOnBoot = true,
            hasMicrophonePermission = hasPermission,
            sdkInt = sdkInt,
        )
    }
}
