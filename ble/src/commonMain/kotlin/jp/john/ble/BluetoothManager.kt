package jp.john.ble


import kotlinx.coroutines.flow.StateFlow

interface BluetoothManager {

    /**
     * 蓝牙状态流
     */
    val state: StateFlow<BluetoothState>

    /**
     * 扫描到的设备流
     */
    val discoveredDevices: StateFlow<List<BluetoothDevice>>

    /**
     * 检查蓝牙是否可用
     */
    suspend fun isBluetoothAvailable(): Boolean

    /**
     * 检查蓝牙是否已开启
     */
    suspend fun isBluetoothEnabled(): Boolean

    /**
     * 请求启用蓝牙
     */
    suspend fun enableBluetooth(): BluetoothResult<Unit>

    /**
     * 开始扫描 BLE 设备
     * @param timeoutMillis 扫描超时时间，默认10秒
     * @param serviceUUIDs 可选的服务UUID过滤
     */
    suspend fun startScan(
        timeoutMillis: Long = 10_000L,
        serviceUUIDs: List<String>? = null
    ): BluetoothResult<Unit>

    /**
     * 停止扫描
     */
    suspend fun stopScan()

    /**
     * 连接到指定设备
     * @param deviceId 设备ID
     * @param timeoutMillis 连接超时时间，默认30秒
     */
    suspend fun connect(
        deviceId: String,
        timeoutMillis: Long = 30_000L
    ): BluetoothResult<BluetoothDevice>

    /**
     * 断开连接
     */
    suspend fun disconnect(): BluetoothResult<Unit>

    /**
     * 获取已连接的设备
     */
    suspend fun getConnectedDevice(): BluetoothDevice?

    /**
     * 清理资源
     */
    fun cleanup()
}

expect fun createBluetoothManager(): BluetoothManager
