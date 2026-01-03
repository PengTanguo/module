package jp.john.log


import android.util.Log

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    val finalMessage = if (throwable != null) {
        "$message\n${Log.getStackTraceString(throwable)}"
    } else {
        message
    }

    when (level) {
        LogLevel.VERBOSE -> Log.v(tag, finalMessage)
        LogLevel.DEBUG -> Log.d(tag, finalMessage)
        LogLevel.INFO -> Log.i(tag, finalMessage)
        LogLevel.WARN -> Log.w(tag, finalMessage)
        LogLevel.ERROR -> Log.e(tag, finalMessage)
        LogLevel.ASSERT -> Log.wtf(tag, finalMessage)
    }
}

actual fun getStackTrace(): List<String> {
    return Thread.currentThread().stackTrace.map { element ->
        "${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
    }
}

actual fun getCurrentThreadInfo(): ThreadInfo {
    val thread = Thread.currentThread()
    return ThreadInfo(thread.name, thread.id.toString())
}
