package jp.john.log

import platform.Foundation.*
import kotlinx.cinterop.*
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
actual class PlatformLogWriter actual constructor(private val config: LogConfig) : LogWriter {

    private val fileManager = NSFileManager.defaultManager
    private val dateFormatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    }
    private val fileDateFormatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd"
    }

    private val logDir: String by lazy {
        config.logFilePath ?: run {
            val documentsPath = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            ).firstOrNull() as? String ?: ""
            "$documentsPath/AppLogs"
        }.also { path ->
            fileManager.createDirectoryAtPath(
                path,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }
    }

    private val currentLogFile: String
        get() {
            val fileName = "log_${fileDateFormatter.stringFromDate(NSDate())}.txt"
            return "$logDir/$fileName"
        }

    override fun writeLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        try {
            val filePath = currentLogFile

            // 检查文件大小
            if (fileManager.fileExistsAtPath(filePath)) {
                val attrs = fileManager.attributesOfItemAtPath(filePath, error = null)
                val fileSize = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0
                if (fileSize > config.maxFileSize) {
                    rotateLogFile(filePath)
                }
            }

            val timestamp = dateFormatter.stringFromDate(NSDate())
            val levelStr = level.name.first()
            val logLine = "$timestamp $levelStr/$tag: $message\n"

            val logContent = logLine + (throwable?.stackTraceToString()?.plus("\n") ?: "")
            val nsData = logContent.toNSData()

            if (fileManager.fileExistsAtPath(filePath)) {
                val fileHandle = NSFileHandle.fileHandleForWritingAtPath(filePath)
                fileHandle?.seekToEndOfFile()
                fileHandle?.writeData(nsData)
                fileHandle?.closeFile()
            } else {
                fileManager.createFileAtPath(filePath, contents = nsData, attributes = null)
            }

            cleanOldLogs()

        } catch (e: Exception) {
            NSLog("LogWriter error: ${e.message}")
        }
    }

    private fun rotateLogFile(filePath: String) {
        val timestamp = NSDate().timeIntervalSince1970.toLong()
        val file = filePath.substringAfterLast("/")
        val nameWithoutExt = file.substringBeforeLast(".")
        val ext = file.substringAfterLast(".")
        val newPath = "$logDir/${nameWithoutExt}_$timestamp.$ext"
        fileManager.moveItemAtPath(filePath, toPath = newPath, error = null)
    }

    override fun cleanOldLogs() {
        try {
            val files = fileManager.contentsOfDirectoryAtPath(logDir, error = null) as? List<*>
            val sortedFiles = files?.mapNotNull { it as? String }
                ?.map { "$logDir/$it" }
                ?.sortedByDescending { path ->
                    val attrs = fileManager.attributesOfItemAtPath(path, error = null)
                    (attrs?.get(NSFileModificationDate) as? NSDate)?.timeIntervalSince1970 ?: 0.0
                } ?: emptyList()

            sortedFiles.drop(config.maxFileCount).forEach { path ->
                fileManager.removeItemAtPath(path, error = null)
            }
        } catch (e: Exception) {
            NSLog("Clean logs error: ${e.message}")
        }
    }

    override fun getLogFiles(): List<String> {
        return try {
            val files = fileManager.contentsOfDirectoryAtPath(logDir, error = null) as? List<*>
            files?.mapNotNull { it as? String }
                ?.map { "$logDir/$it" }
                ?.sortedByDescending { path ->
                    val attrs = fileManager.attributesOfItemAtPath(path, error = null)
                    (attrs?.get(NSFileModificationDate) as? NSDate)?.timeIntervalSince1970 ?: 0.0
                } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun clearAllLogs() {
        try {
            val files = fileManager.contentsOfDirectoryAtPath(logDir, error = null) as? List<*>
            files?.mapNotNull { it as? String }?.forEach { file ->
                fileManager.removeItemAtPath("$logDir/$file", error = null)
            }
        } catch (e: Exception) {
            NSLog("Clear all logs error: ${e.message}")
        }
    }

    private fun String.toNSData(): NSData {
        val bytes = this.encodeToByteArray()
        return bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    }
}
