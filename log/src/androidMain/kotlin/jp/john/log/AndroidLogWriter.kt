package jp.john.log


import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

actual class PlatformLogWriter actual constructor(private val config: LogConfig) : LogWriter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val logDir: File by lazy {
        val path = config.logFilePath ?: run {
            val externalDir = Environment.getExternalStorageDirectory()
            "${externalDir.absolutePath}/AppLogs"
        }
        File(path).apply { mkdirs() }
    }

    private val currentLogFile: File
        get() {
            val fileName = "log_${fileDateFormat.format(Date())}.txt"
            return File(logDir, fileName)
        }

    override fun writeLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        try {
            val file = currentLogFile

            // 检查文件大小
            if (file.exists() && file.length() > config.maxFileSize) {
                rotateLogFile(file)
            }

            FileWriter(file, true).use { writer ->
                val timestamp = dateFormat.format(Date())
                val levelStr = level.name.first()
                writer.append("$timestamp $levelStr/$tag: $message\n")

                throwable?.let {
                    writer.append("${it.stackTraceToString()}\n")
                }
            }

            // 清理旧文件
            cleanOldLogs()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rotateLogFile(file: File) {
        val timestamp = System.currentTimeMillis()
        val newName = "${file.nameWithoutExtension}_$timestamp.${file.extension}"
        file.renameTo(File(file.parent, newName))
    }

    override fun cleanOldLogs() {
        try {
            val files = logDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
            files.drop(config.maxFileCount).forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getLogFiles(): List<String> {
        return try {
            logDir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.map { it.absolutePath }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun clearAllLogs() {
        try {
            logDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
