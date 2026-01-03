package  jp.john.ble

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.NSObject

actual fun createBluetoothManager(): BluetoothManager {
    return IOSBluetoothManager()
}

@OptIn(ExperimentalForeignApi::class)
class IOSBluetoothManager : BluetoothManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<BluetoothState>(BluetoothState.Idle)
    override val state: StateFlow<BluetoothState> = _state.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val deviceMap = mutableMapOf<String, BluetoothDevice>()
    private var connectedDevice: BluetoothDevice? = null
    private var scanJob: Job? = null
    private var connectionJob: Job? = null
    private var connectionContinuation: CancellableContinuation<BluetoothResult<BluetoothDevice>>? = null

    private val centralManagerDelegate = CentralManagerDelegate(this)
    private val centralManager = CBCentralManager(
        delegate = centralManagerDelegate,
        queue = null
    )

    override suspend fun isBluetoothAvailable(): Boolean {
        return true // iOS 总是有蓝牙硬件
    }

    override suspend fun isBluetoothEnabled(): Boolean {
        return when (centralManager.state) {
            CBManagerStatePoweredOn -> true
            else -> false
        }
    }

    override suspend fun enableBluetooth(): BluetoothResult<Unit> {
        return if (isBluetoothEnabled()) {
            BluetoothResult.Success(Unit)
        } else {
            BluetoothResult.Error(BluetoothError.BluetoothNotEnabled)
        }
    }

    override suspend fun startScan(
        timeoutMillis: Long,
        serviceUUIDs: List<String>?
    ): BluetoothResult<Unit> {
        if (!isBluetoothEnabled()) {
            return BluetoothResult.Error(BluetoothError.BluetoothNotEnabled)
        }

        stopScanInternal()

        _state.value = BluetoothState.Scanning
        deviceMap.clear()
        _discoveredDevices.value = emptyList()

        val services = serviceUUIDs?.map { uuid ->
            CBUUID.UUIDWithString(uuid)
        }

        val options = mapOf(
            CBCentralManagerScanOptionAllowDuplicatesKey to true
        )

        centralManager.scanForPeripheralsWithServices(
            services,
            options
        )

        // 设置扫描超时
        scanJob = scope.launch {
            delay(timeoutMillis)
            stopScan()
        }

        return BluetoothResult.Success(Unit)
    }

    override suspend fun stopScan() {
        stopScanInternal()
        if (_state.value == BluetoothState.Scanning) {
            _state.value = BluetoothState.Idle
        }
    }

    private fun stopScanInternal() {
        scanJob?.cancel()
        scanJob = null

        if (centralManager.isScanning) {
            centralManager.stopScan()
        }
    }

    override suspend fun connect(
        deviceId: String,
        timeoutMillis: Long
    ): BluetoothResult<BluetoothDevice> {
        if (!isBluetoothEnabled()) {
            return BluetoothResult.Error(BluetoothError.BluetoothNotEnabled)
        }

        stopScanInternal()
        disconnectInternal()

        _state.value = BluetoothState.Connecting

        return suspendCancellableCoroutine { continuation ->
            connectionContinuation = continuation

            val timeoutJob = scope.launch {
                delay(timeoutMillis)
                if (continuation.isActive) {
                    connectionContinuation = null
                    disconnectInternal()
                    _state.value = BluetoothState.Disconnected
                    continuation.resume(
                        BluetoothResult.Error(
                            BluetoothError.ConnectionFailed("Connection timeout")
                        )
                    ) {}
                }
            }

            continuation.invokeOnCancellation {
                timeoutJob.cancel()
                connectionContinuation = null
                disconnectInternal()
            }

            val peripheral = deviceMap[deviceId]?.let { device ->
                centralManager.retrievePeripheralsWithIdentifiers(
                    listOf(NSUUID(deviceId))
                ).firstOrNull() as? CBPeripheral
            }

            if (peripheral == null) {
                timeoutJob.cancel()
                connectionContinuation = null
                _state.value = BluetoothState.Disconnected
                continuation.resume(
                    BluetoothResult.Error(
                        BluetoothError.ConnectionFailed("Device not found")
                    )
                ) {}
                return@suspendCancellableCoroutine
            }

            centralManager.connectPeripheral(peripheral, null)
        }
    }

    override suspend fun disconnect(): BluetoothResult<Unit> {
        return try {
            disconnectInternal()
            _state.value = BluetoothState.Disconnected
            BluetoothResult.Success(Unit)
        } catch (e: Exception) {
            BluetoothResult.Error(
                BluetoothError.Unknown(e.message ?: "Disconnect error")
            )
        }
    }

    private fun disconnectInternal() {
        connectionJob?.cancel()
        connectionJob = null

        connectedDevice?.let { device ->
            val peripherals = centralManager.retrievePeripheralsWithIdentifiers(
                listOf(NSUUID(device.id))
            )
            peripherals.forEach { peripheral ->
                centralManager.cancelPeripheralConnection(peripheral as CBPeripheral)
            }
        }

        connectedDevice = null
    }

    override suspend fun getConnectedDevice(): BluetoothDevice? {
        return connectedDevice
    }

    override fun cleanup() {
        scope.launch {
            stopScanInternal()
            disconnectInternal()
            scope.cancel()
        }
    }

    internal fun onDeviceDiscovered(
        peripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        rssi: NSNumber
    ) {
        val deviceId = peripheral.identifier.UUIDString
        val deviceName = peripheral.name ?: advertisementData[CBAdvertisementDataLocalNameKey] as? String ?: "Unknown Device"

        val bleDevice = BluetoothDevice(
            id = deviceId,
            name = deviceName,
            rssi = rssi.intValue,
            isConnectable = true
        )

        deviceMap[deviceId] = bleDevice
        _discoveredDevices.value = deviceMap.values.toList()
    }

    internal fun onDeviceConnected(peripheral: CBPeripheral) {
        val deviceId = peripheral.identifier.UUIDString
        val deviceName = peripheral.name ?: "Unknown Device"

        val bleDevice = BluetoothDevice(
            id = deviceId,
            name = deviceName,
            rssi = 0,
            isConnectable = true
        )

        connectedDevice = bleDevice
        _state.value = BluetoothState.Connected(bleDevice)

        connectionContinuation?.let { continuation ->
            if (continuation.isActive) {
                connectionContinuation = null
                continuation.resume(BluetoothResult.Success(bleDevice)) {}
            }
        }

        // 发现服务
        peripheral.discoverServices(null)
    }

    internal fun onDeviceDisconnected(peripheral: CBPeripheral, error: NSError?) {
        val wasConnecting = _state.value is BluetoothState.Connecting

        connectedDevice = null
        _state.value = BluetoothState.Disconnected

        connectionContinuation?.let { continuation ->
            if (continuation.isActive) {
                connectionContinuation = null
                continuation.resume(
                    BluetoothResult.Error(
                        BluetoothError.ConnectionFailed(
                            error?.localizedDescription ?: "Connection failed"
                        )
                    )
                ) {}
            }
        }

        if (!wasConnecting && error != null) {
            // 连接后断开，通知连接丢失
        }
    }

    internal fun onDeviceConnectionFailed(peripheral: CBPeripheral, error: NSError?) {
        connectedDevice = null
        _state.value = BluetoothState.Disconnected

        connectionContinuation?.let { continuation ->
            if (continuation.isActive) {
                connectionContinuation = null
                continuation.resume(
                    BluetoothResult.Error(
                        BluetoothError.ConnectionFailed(
                            error?.localizedDescription ?: "Connection failed"
                        )
                    )
                ) {}
            }
        }
    }

    @Suppress("CONFLICTING_OVERLOADS")
    private class CentralManagerDelegate(
        private val manager: IOSBluetoothManager
    ) : NSObject(), CBCentralManagerDelegateProtocol {

        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            // 状态更新处理
        }

        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber
        ) {
            manager.onDeviceDiscovered(didDiscoverPeripheral, advertisementData, RSSI)
        }

        override fun centralManager(
            central: CBCentralManager,
            didConnectPeripheral: CBPeripheral
        ) {
            manager.onDeviceConnected(didConnectPeripheral)
        }

        override fun centralManager(
            central: CBCentralManager,
            didDisconnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            manager.onDeviceDisconnected(didDisconnectPeripheral, error)
        }

        override fun centralManager(
            central: CBCentralManager,
            didFailToConnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            manager.onDeviceConnectionFailed(didFailToConnectPeripheral, error)
        }
    }
}
