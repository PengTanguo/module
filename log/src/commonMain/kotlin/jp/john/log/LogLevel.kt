package jp.john.log


enum class LogLevel(val priority: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    ASSERT(5);

    companion object {
        fun fromString(level: String): LogLevel {
            return entries.find { it.name.equals(level, ignoreCase = true) } ?: DEBUG
        }
    }
}
