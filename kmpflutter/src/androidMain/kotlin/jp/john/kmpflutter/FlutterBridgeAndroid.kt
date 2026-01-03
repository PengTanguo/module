package jp.john.kmpflutter


import android.content.Context
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel
import io.flutter.embedding.engine.FlutterEngine

class FlutterBridgeAndroid(
    private val flutterEngine: FlutterEngine
) {
    private val bridge = FlutterBridge()
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null

    companion object {
        const val METHOD_CHANNEL = "com.example.kmpbridge/method"
        const val EVENT_CHANNEL = "com.example.kmpbridge/event"
    }

    fun initialize() {
        // Method Channel - 双向方法调用
        methodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            METHOD_CHANNEL
        )

        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "sendMessage" -> {
                    val jsonString = call.argument<String>("data") ?: ""
                    val response = bridge.receiveFromFlutter(jsonString)
                    result.success(response)
                }
                else -> result.notImplemented()
            }
        }

        // Event Channel - Native 主动推送
        eventChannel = EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            EVENT_CHANNEL
        )

        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
                bridge.registerFlutterCallback { message ->
                    eventSink?.success(message)
                }
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })
    }

    // 主动调用 Flutter 方法
    fun callFlutterMethod(method: String, data: Map<String, Any>) {
        methodChannel.invokeMethod(method, data)
    }

    fun dispose() {
        bridge.dispose()
    }
}
