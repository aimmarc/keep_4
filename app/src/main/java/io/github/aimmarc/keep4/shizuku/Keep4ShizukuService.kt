package io.github.aimmarc.keep4.shizuku

import android.content.Context
import androidx.annotation.Keep
import kotlin.system.exitProcess

class Keep4ShizukuService @Keep @JvmOverloads constructor(
    @Suppress("UNUSED_PARAMETER") context: Context? = null,
) : IKeep4ShizukuService.Stub() {

    override fun applyBackgroundOptimization(packageName: String): String {
        require(packageName.matches(PACKAGE_NAME_PATTERN)) { "Invalid package name" }
        val commands = listOf(
            arrayOf("dumpsys", "deviceidle", "whitelist", "+$packageName"),
            arrayOf("cmd", "appops", "set", packageName, "RUN_IN_BACKGROUND", "allow"),
            arrayOf("cmd", "appops", "set", packageName, "RUN_ANY_IN_BACKGROUND", "allow"),
        )
        val failures = commands.mapNotNull(::execute)
        return if (failures.isEmpty()) "后台优化已应用" else "部分操作失败：${failures.joinToString()}"
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun execute(command: Array<String>): String? {
        return runCatching {
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) null else error.ifBlank { output.ifBlank { "退出码 $exitCode" } }
        }.getOrElse { it.message ?: "命令执行失败" }
    }

    private companion object {
        val PACKAGE_NAME_PATTERN = Regex("[A-Za-z0-9_.]+")
    }
}
