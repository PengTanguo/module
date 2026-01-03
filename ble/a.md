// 初始化 (Android)
class MyApplication : Application() {
override fun onCreate() {
super.onCreate()
AndroidBluetoothManager.initialize(this)
}
}

// 使用
class BluetoothViewModel : ViewModel() {
private val bluetoothManager = createBluetoothManager()

    val state = bluetoothManager.state
    val devices = bluetoothManager.discoveredDevices
    
    fun startScan() {
        viewModelScope.launch {
            when (val result = bluetoothManager.startScan(timeoutMillis = 10_000)) {
                is BluetoothResult.Success -> {
                    println("扫描开始")
                }
                is BluetoothResult.Error -> {
                    println("扫描失败: ${result.error}")
                }
            }
        }
    }
    
    fun stopScan() {
        viewModelScope.launch {
            bluetoothManager.stopScan()
        }
    }
    
    fun connect(deviceId: String) {
        viewModelScope.launch {
            when (val result = bluetoothManager.connect(deviceId)) {
                is BluetoothResult.Success -> {
                    println("连接成功: ${result.data.name}")
                }
                is BluetoothResult.Error -> {
                    println("连接失败: ${result.error}")
                }
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            bluetoothManager.disconnect()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
    }
}
