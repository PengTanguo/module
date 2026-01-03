package jp.john.log

import platform.Foundation.NSLog
import platform.Foundation.NSThread

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    val levelPrefix = when (level) {
        LogLevel.VERBOSE -> "💜 V"
        LogLevel.DEBUG -> "💚 D"
        LogLevel.INFO -> "💙 I"
        LogLevel.WARN -> "💛 W"
        LogLevel.ERROR -> "❤️ E"
        LogLevel.ASSERT -> "💔 A"
    }

    val finalMessage = buildString {
        append("$levelPrefix/$tag: $message")
        throwable?.let {
            append("\n${it.stackTraceToString()}")
        }
    }

    NSLog(finalMessage)
}

@OptIn(ExperimentalStdlibApi::class)
actual fun getStackTrace(): List<String> {
    return try {
        // iOS 上直接使用 getStackTraceAddresses 或解析字符串
        val exception = Exception()
        val stackString = exception.stackTraceToString()

        // 解析堆栈字符串，格式通常是：
        // at <function> (<file>:<line>:<column>)
        stackString.lines()
            .filter { it.trim().startsWith("at ") }
            .map { it.trim().removePrefix("at ").trim() }
            .filter { it.isNotEmpty() }
    } catch (e: Exception) {
        emptyList()
    }
}

actual fun getCurrentThreadInfo(): ThreadInfo {
    val thread = NSThread.currentThread
    val name = thread.name ?: "Unknown"
    val isMain = thread.isMainThread
    val id = if (isMain) "main" else thread.hashCode().toString()
    return ThreadInfo(name, id)
}
