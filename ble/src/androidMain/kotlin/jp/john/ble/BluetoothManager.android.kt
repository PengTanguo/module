package jp.john.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

actual fun createBluetoothManager(): BluetoothManager {
    return AndroidBluetoothManager.instance
}

class AndroidBluetoothManager private constructor(
    private val context: Context
) : BluetoothManager {

    companion object {
        @Volatile
        private var INSTANCE: AndroidBluetoothManager? = null

        val instance: AndroidBluetoothManager
            get() = INSTANCE ?: throw IllegalStateException(
                "AndroidBluetoothManager must be initialized first. " +
                        "Call AndroidBluetoothManager.initialize(context) in your Application class."
            )

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = AndroidBluetoothManager(context.applicationContext)
                    }
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val bluetoothManager: AndroidBluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as AndroidBluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val _state = MutableStateFlow<BluetoothState>(BluetoothState.Idle)
    override val state: StateFlow<BluetoothState> = _state.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val deviceMap = mutableMapOf<String, BluetoothDevice>()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private var scanJob: Job? = null
    private var connectionJob: Job? = null

    override suspend fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    override suspend fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    override suspend fun enableBluetooth(): BluetoothResult<Unit> {
        return withContext(Dispatchers.Main) {
            if (!isBluetoothAvailable()) {
                return@withContext BluetoothResult.Error(BluetoothError.BluetoothNotAvailable)
            }

            if (isBluetoothEnabled()) {
                return@withContext BluetoothResult.Success(Unit)
            }

            // Android 12+ 需要 BLUETOOTH_CONNECT 权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    return@withContext BluetoothResult.Error(BluetoothError.BluetoothPermissionDenied)
                }
            }

            try {
                @SuppressLint("MissingPermission")
                bluetoothAdapter?.enable()
                BluetoothResult.Success(Unit)
            } catch (e: Exception) {
                BluetoothResult.Error(BluetoothError.Unknown(e.message ?: "Failed to enable Bluetooth"))
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun startScan(
        timeoutMillis: Long,
        serviceUUIDs: List<String>?
    ): BluetoothResult<Unit> {
        return withContext(Dispatchers.Main) {
            // 检查权限
            if (!checkScanPermissions()) {
                return@withContext BluetoothResult.Error(BluetoothError.BluetoothPermissionDenied)
            }

            if (!isBluetoothEnabled()) {
                return@withContext BluetoothResult.Error(BluetoothError.BluetoothNotEnabled)
            }

            if (bluetoothLeScanner == null) {
                return@withContext BluetoothResult.Error(BluetoothError.BluetoothNotAvailable)
            }

            // 停止之前的扫描
            stopScanInternal()

            _state.value = BluetoothState.Scanning
            deviceMap.clear()
            _discoveredDevices.value = emptyList()

            try {
                val scanFilters = serviceUUIDs?.map { uuid ->
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(UUID.fromString(uuid)))
                        .build()
                } ?: emptyList()

                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    .setReportDelay(0)
                    .build()

                bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)

                // 设置扫描超时
                scanJob = scope.launch {
                    delay(timeoutMillis)
                    stopScan()
                }

                BluetoothResult.Success(Unit)
            } catch (e: Exception) {
                _state.value = BluetoothState.Idle
                BluetoothResult.Error(
                    BluetoothError.ScanFailed(e.message ?: "Unknown scan error")
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan() {
        withContext(Dispatchers.Main) {
            stopScanInternal()
            if (_state.value == BluetoothState.Scanning) {
                _state.value = BluetoothState.Idle
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        scanJob?.cancel()
        scanJob = null

        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(
        deviceId: String,
        timeoutMillis: Long
    ): BluetoothResult<BluetoothDevice> {
        return withContext(Dispatchers.Main) {
            // 检查权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    return@withContext BluetoothResult.Error(BluetoothError.BluetoothPermissionDenied)
                }
            }

            if (!isBluetoothEnabled()) {
                return@withContext BluetoothResult.Error(BluetoothError.BluetoothNotEnabled)
            }

            // 停止扫描
            stopScanInternal()

            // 断开之前的连接
            disconnectInternal()

            _state.value = BluetoothState.Connecting

            try {
                val device = bluetoothAdapter?.getRemoteDevice(deviceId)
                    ?: return@withContext BluetoothResult.Error(
                        BluetoothError.ConnectionFailed("Device not found")
                    )

                val result = suspendCancellableCoroutine<BluetoothResult<BluetoothDevice>> { continuation ->
                    var isResumed = false

                    val timeoutJob = scope.launch {
                        delay(timeoutMillis)
                        if (!isResumed) {
                            isResumed = true
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
                        disconnectInternal()
                    }

                    val gattCallback = object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(
                            gatt: BluetoothGatt,
                            status: Int,
                            newState: Int
                        ) {
                            when (newState) {
                                BluetoothProfile.STATE_CONNECTED -> {
                                    if (!isResumed && status == BluetoothGatt.GATT_SUCCESS) {
                                        isResumed = true
                                        timeoutJob.cancel()

                                        val bleDevice = BluetoothDevice(
                                            id = device.address,
                                            name = device.name,
                                            rssi = 0,
                                            isConnectable = true
                                        )
                                        connectedDevice = bleDevice
                                        _state.value = BluetoothState.Connected(bleDevice)

                                        // 发现服务
                                        scope.launch(Dispatchers.Main) {
                                            delay(600) // 等待连接稳定
                                            gatt.discoverServices()
                                        }

                                        continuation.resume(BluetoothResult.Success(bleDevice)) {}
                                    }
                                }
                                BluetoothProfile.STATE_DISCONNECTED -> {
                                    if (!isResumed) {
                                        isResumed = true
                                        timeoutJob.cancel()
                                        gatt.close()
                                        bluetoothGatt = null
                                        connectedDevice = null
                                        _state.value = BluetoothState.Disconnected
                                        continuation.resume(
                                            BluetoothResult.Error(
                                                BluetoothError.ConnectionFailed("Connection failed with status: $status")
                                            )
                                        ) {}
                                    } else {
                                        // 连接后断开
                                        gatt.close()
                                        bluetoothGatt = null
                                        connectedDevice = null
                                        _state.value = BluetoothState.Disconnected
                                        scope.launch {
                                            // 通知连接丢失
                                        }
                                    }
                                }
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            // 服务发现完成
                        }
                    }

                    bluetoothGatt = device.connectGatt(
                        context,
                        false,
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                }

                result
            } catch (e: Exception) {
                disconnectInternal()
                _state.value = BluetoothState.Disconnected
                BluetoothResult.Error(
                    BluetoothError.ConnectionFailed(e.message ?: "Unknown connection error")
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect(): BluetoothResult<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                disconnectInternal()
                _state.value = BluetoothState.Disconnected
                BluetoothResult.Success(Unit)
            } catch (e: Exception) {
                BluetoothResult.Error(
                    BluetoothError.Unknown(e.message ?: "Disconnect error")
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal() {
        connectionJob?.cancel()
        connectionJob = null

        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                // Ignore
            }
            bluetoothGatt = null
        }

        connectedDevice = null
    }

    override suspend fun getConnectedDevice(): BluetoothDevice? {
        return connectedDevice
    }

    override fun cleanup() {
        scope.launch(Dispatchers.Main) {
            stopScanInternal()
            disconnectInternal()
            scope.cancel()
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceId = device.address

            val bleDevice = BluetoothDevice(
                id = deviceId,
                name = device.name ?: "Unknown Device",
                rssi = result.rssi,
                isConnectable = result.isConnectable
            )

            deviceMap[deviceId] = bleDevice
            _discoveredDevices.value = deviceMap.values.toList()
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = BluetoothState.Idle
            // 可以通过 SharedFlow 发送错误事件
        }
    }

    private fun checkScanPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
