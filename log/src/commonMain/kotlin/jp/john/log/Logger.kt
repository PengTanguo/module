package jp.john.log



object Logger {
    private var config: LogConfig = LogConfig()
    private var logWriter: LogWriter? = null

    /**
     * 初始化日志配置
     */
    fun init(config: LogConfig) {
        this.config = config
        if (config.enableFileLog) {
            logWriter = PlatformLogWriter(config)
        }
    }

    /**
     * 获取当前配置
     */
    fun getConfig(): LogConfig = config

    /**
     * 更新配置
     */
    fun updateConfig(block: LogConfig.() -> LogConfig) {
        config = config.block()
        if (config.enableFileLog && logWriter == null) {
            logWriter = PlatformLogWriter(config)
        } else if (!config.enableFileLog) {
            logWriter = null
        }
    }

    // ==================== 基础日志方法 ====================

    fun v(tag: String = config.defaultTag, message: String, throwable: Throwable? = null) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }

    fun d(tag: String = config.defaultTag, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }

    fun i(tag: String = config.defaultTag, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }

    fun w(tag: String = config.defaultTag, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String = config.defaultTag, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    fun wtf(tag: String = config.defaultTag, message: String, throwable: Throwable? = null) {
        log(LogLevel.ASSERT, tag, message, throwable)
    }

    // ==================== 内部实现 ====================

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!config.isEnabled || level.priority < config.minLevel.priority) {
            return
        }

        safeCall {
            val finalMessage = if (config.showStackTrace) {
                val location = getCallerInfo()
                "[$location] $message"
            } else {
                message
            }

            // 控制台输出
            platformLog(level, tag, finalMessage, throwable)

            // 文件输出
            logWriter?.writeLog(level, tag, finalMessage, throwable)
        }
    }

    /**
     * 获取调用者信息
     */
    private fun getCallerInfo(): String {
        return try {
            val stackTrace = getStackTrace()
            // 跳过 Logger 内部调用
            val caller = stackTrace.firstOrNull { element ->
                !element.contains("Logger") &&
                        !element.contains("getCallerInfo") &&
                        !element.contains("getStackTrace")
            } ?: "Unknown"
            caller
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // ==================== 参数日志 ====================

    /**
     * 打印参数日志
     */
    fun log(vararg params: Pair<String?, Any?>) {
        if (!config.isEnabled) return

        safeCall {
            val location = getCallerInfo()

            if (params.isEmpty()) {
                d(message = "[$location] (no parameters)")
                return@safeCall
            }

            val paramStr = params.mapNotNull { (key, value) ->
                key?.let { "$it=${formatValue(value)}" }
            }.joinToString(", ")

            d(message = "[$location] $paramStr")
        }
    }

    /**
     * 格式化参数值
     */
    private fun formatValue(value: Any?): String {
        return try {
            when (value) {
                null -> "null"
                is String -> "\"$value\""
                is CharSequence -> "\"$value\""
                is Char -> "'$value'"
                is Number -> value.toString()
                is Boolean -> value.toString()
                is Array<*> -> value.contentToString()
                is ByteArray -> "ByteArray[${value.size}]"
                is Collection<*> -> value.toString()
                is Map<*, *> -> value.toString()
                else -> value.toString()
            }
        } catch (e: Exception) {
            "error: ${e.message}"
        }
    }

    // ==================== 堆栈跟踪 ====================

    /**
     * 打印调用栈信息
     */
    fun logTrace(
        tag: String = config.defaultTag,
        maxDepth: Int = -1,
        skipSystem: Boolean = true,
        format: Boolean = config.formatOutput
    ) {
        if (!config.isEnabled) return

        safeCall {
            val stackTrace = getStackTrace()
            val threadInfo = getCurrentThreadInfo()

            // 打印头部
            if (format) {
                d(tag, "┌────────────────────────────────────────────")
                d(tag, "│ Thread: ${threadInfo.name} (id=${threadInfo.id})")
                d(tag, "├────────────────────────────────────────────")
            }

            var count = 0
            val filteredStack = stackTrace
                .drop(2) // 跳过 logTrace 和 getStackTrace
                .let { if (skipSystem) it.filter { !isSystemClass(it) } else it }
                .let { if (maxDepth > 0) it.take(maxDepth) else it }

            filteredStack.forEach { element ->
                val prefix = if (format) "│ " else "at "
                d(tag, "  $prefix$element")
                count++
            }

            // 打印尾部
            if (format) {
                if (count == 0) {
                    d(tag, "│ (No application stack trace)")
                }
                d(tag, "└────────────────────────────────────────────")
            }
        }
    }

    private fun isSystemClass(className: String): Boolean {
        return className.startsWith("java.") ||
                className.startsWith("javax.") ||
                className.startsWith("android.") ||
                className.startsWith("androidx.") ||
                className.startsWith("com.android.") ||
                className.startsWith("dalvik.") ||
                className.startsWith("sun.") ||
                className.startsWith("kotlin.") ||
                className.startsWith("kotlinx.") ||
                className.startsWith("platform.") ||
                className.startsWith("Foundation.")
    }

    // ==================== 文件日志管理 ====================

    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): List<String> = logWriter?.getLogFiles() ?: emptyList()

    /**
     * 清理旧日志
     */
    fun cleanOldLogs() = logWriter?.cleanOldLogs()

    /**
     * 清空所有日志
     */
    fun clearAllLogs() = logWriter?.clearAllLogs()

    // ==================== 平台相关方法（expect） ====================

    private fun safeCall(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            // 日志系统本身的错误，使用平台默认输出
            platformLog(LogLevel.ERROR, "Logger", "Logger internal error: ${e.message}", e)
        }
    }
}

// Platform specific functions
expect fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?)
expect fun getStackTrace(): List<String>
expect fun getCurrentThreadInfo(): ThreadInfo

data class ThreadInfo(val name: String, val id: String)
