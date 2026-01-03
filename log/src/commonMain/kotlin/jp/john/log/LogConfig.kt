package jp.john.log


data class LogConfig(
    /**
     * 是否启用日志
     */
    val isEnabled: Boolean = true,

    /**
     * 最低日志级别
     */
    val minLevel: LogLevel = LogLevel.VERBOSE,

    /**
     * 是否启用文件日志
     */
    val enableFileLog: Boolean = false,

    /**
     * 日志文件路径（平台相关）
     */
    val logFilePath: String? = null,

    /**
     * 单个日志文件最大大小（字节），默认 5MB
     */
    val maxFileSize: Long = 5 * 1024 * 1024,

    /**
     * 最多保留日志文件数量
     */
    val maxFileCount: Int = 3,

    /**
     * 是否打印调用栈信息
     */
    val showStackTrace: Boolean = true,

    /**
     * 是否格式化输出
     */
    val formatOutput: Boolean = true,

    /**
     * 默认 Tag
     */
    val defaultTag: String = "AppLog"
)
