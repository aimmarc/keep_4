package io.github.aimmarc.keep4.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    UNAVAILABLE,
    PERMISSION_REQUIRED,
    READY,
}

data class ShizukuSnapshot(
    val status: ShizukuStatus = ShizukuStatus.UNAVAILABLE,
    val result: String? = null,
    val working: Boolean = false,
)

class ShizukuManager(context: Context) {
    private val appContext = context.applicationContext
    private val mutableSnapshot = MutableStateFlow(ShizukuSnapshot())
    val snapshot = mutableSnapshot.asStateFlow()

    @Volatile
    private var remoteService: IKeep4ShizukuService? = null
    private var connectionResult: CompletableDeferred<IKeep4ShizukuService>? = null
    private var binding = false

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(appContext.packageName, Keep4ShizukuService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("keep4_shizuku")
        .version(1)

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val service = IKeep4ShizukuService.Stub.asInterface(binder)
            remoteService = service
            binding = false
            connectionResult?.complete(service)
            connectionResult = null
        }

        override fun onServiceDisconnected(name: ComponentName) {
            remoteService = null
            binding = false
        }
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
        if (requestCode == REQUEST_CODE) refresh()
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refresh()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        remoteService = null
        binding = false
        connectionResult?.completeExceptionally(IllegalStateException("Shizuku 服务已断开"))
        connectionResult = null
        mutableSnapshot.value = mutableSnapshot.value.copy(
            status = ShizukuStatus.UNAVAILABLE,
            working = false,
        )
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        refresh()
    }

    fun refresh() {
        val status = runCatching {
            when {
                !Shizuku.pingBinder() -> ShizukuStatus.UNAVAILABLE
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuStatus.READY
                else -> ShizukuStatus.PERMISSION_REQUIRED
            }
        }.getOrDefault(ShizukuStatus.UNAVAILABLE)
        mutableSnapshot.value = mutableSnapshot.value.copy(status = status, working = false)
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder()) {
            refresh()
            return
        }
        Shizuku.requestPermission(REQUEST_CODE)
    }

    fun setMessage(message: String) {
        mutableSnapshot.value = mutableSnapshot.value.copy(result = message)
    }

    suspend fun applyBackgroundOptimization() {
        if (mutableSnapshot.value.status != ShizukuStatus.READY) return
        mutableSnapshot.value = mutableSnapshot.value.copy(working = true, result = null)
        val result = runCatching {
            val service = awaitUserService()
            withContext(Dispatchers.IO) {
                service.applyBackgroundOptimization(appContext.packageName)
            }
        }.getOrElse { error -> error.message ?: "Shizuku 后台优化失败" }
        mutableSnapshot.value = mutableSnapshot.value.copy(working = false, result = result)
    }

    fun close() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        if (binding || remoteService != null) {
            runCatching { Shizuku.unbindUserService(userServiceArgs, userServiceConnection, false) }
        }
        connectionResult?.cancel()
        connectionResult = null
        remoteService = null
        binding = false
    }

    private suspend fun awaitUserService(): IKeep4ShizukuService {
        remoteService?.let { service ->
            if (service.asBinder().pingBinder()) return service
        }
        val deferred = CompletableDeferred<IKeep4ShizukuService>()
        connectionResult = deferred
        withContext(Dispatchers.Main.immediate) {
            if (!binding) {
                binding = true
                try {
                    Shizuku.bindUserService(userServiceArgs, userServiceConnection)
                } catch (error: Throwable) {
                    binding = false
                    connectionResult = null
                    deferred.completeExceptionally(error)
                }
            }
        }
        return withTimeout(USER_SERVICE_TIMEOUT_MS) { deferred.await() }
    }

    private companion object {
        const val REQUEST_CODE = 4401
        const val USER_SERVICE_TIMEOUT_MS = 10_000L
    }
}
