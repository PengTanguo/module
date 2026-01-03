package jp.john.kmpflutter


import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class Message(
    val id: String,
    val method: String,
    val data: Map<String, String>
)

@Serializable
data class Response(
    val success: Boolean,
    val data: String? = null,
    val error: String? = null
)

// 平台接口定义
interface FlutterBridgeInterface {
    fun sendToFlutter(method: String, data: Map<String, String>)
    fun registerCallback(callback: (String) -> Unit)
}

class FlutterBridge {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private var flutterCallback: ((String) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 注册 Flutter 回调
    fun registerFlutterCallback(callback: (String) -> Unit) {
        flutterCallback = callback
    }

    // 发送消息到 Flutter
    fun sendToFlutter(method: String, data: Map<String, String>) {
        val message = Message(
            id = generateId(),
            method = method,
            data = data
        )

        val jsonString = json.encodeToString(message)
        flutterCallback?.invoke(jsonString)
    }

    // 接收来自 Flutter 的消息
    fun receiveFromFlutter(jsonString: String): String {
        return try {
            val message = json.decodeFromString<Message>(jsonString)
            val result = handleMessage(message)
            json.encodeToString(Response(success = true, data = result))
        } catch (e: Exception) {
            json.encodeToString(Response(success = false, error = e.message))
        }
    }

    // 处理消息
    private fun handleMessage(message: Message): String {
        return when (message.method) {
            "getData" -> {
                val key = message.data["key"] ?: ""
                "Value for $key"
            }
            "calculate" -> {
                val a = message.data["a"]?.toIntOrNull() ?: 0
                val b = message.data["b"]?.toIntOrNull() ?: 0
                (a + b).toString()
            }
            "asyncTask" -> {
                scope.launch {
                    delay(2000)
                    sendToFlutter("taskComplete", mapOf("result" to "Task finished"))
                }
                "Task started"
            }
            else -> "Unknown method: ${message.method}"
        }
    }

    private fun generateId(): String {
        return System.currentTimeMillis().toString()
    }

    fun dispose() {
        scope.cancel()
    }
}
