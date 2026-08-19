package io.github.aimmarc.keep4.receiver

enum class BootAction {
    IGNORE,
    START_SERVICE,
    SHOW_RECOVERY_NOTIFICATION,
}

object BootStartPolicy {
    private const val ANDROID_14_API = 34

    fun decide(
        enabled: Boolean,
        startOnBoot: Boolean,
        hasMicrophonePermission: Boolean,
        sdkInt: Int,
    ): BootAction {
        if (!enabled || !startOnBoot) return BootAction.IGNORE
        if (!hasMicrophonePermission || sdkInt >= ANDROID_14_API) {
            return BootAction.SHOW_RECOVERY_NOTIFICATION
        }
        return BootAction.START_SERVICE
    }
}
