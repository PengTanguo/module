package jp.john.log


interface LogWriter {
    /**
     * 写入日志到文件
     */
    fun writeLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)

    /**
     * 清理旧日志文件
     */
    fun cleanOldLogs()

    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): List<String>

    /**
     * 删除所有日志
     */
    fun clearAllLogs()
}

expect class PlatformLogWriter(config: LogConfig) : LogWriter
