package jp.john.kmpflutter


import platform.Foundation.NSString
import platform.Foundation.create

class FlutterBridgeIOS {
    private val bridge = FlutterBridge()

    // 接收来自 Flutter 的消息
    fun receiveFromFlutter(jsonString: String): String {
        return bridge.receiveFromFlutter(jsonString)
    }

    // 注册回调
    fun registerCallback(callback: (String) -> Unit) {
        bridge.registerFlutterCallback(callback)
    }

    // 主动发送到 Flutter
    fun sendToFlutter(method: String, data: Map<String, String>) {
        bridge.sendToFlutter(method, data)
    }

    fun dispose() {
        bridge.dispose()
    }
}

// iOS 桥接对象
class FlutterBridgeIOSWrapper {
    private val bridge = FlutterBridgeIOS()

    fun handleMethodCall(method: String, arguments: String): String {
        return bridge.receiveFromFlutter(arguments)
    }

    fun setEventCallback(callback: (String) -> Unit) {
        bridge.registerCallback(callback)
    }
}
