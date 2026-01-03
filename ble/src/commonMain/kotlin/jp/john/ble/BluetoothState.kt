package jp.john.ble


sealed class BluetoothState {
    object Idle : BluetoothState()
    object Scanning : BluetoothState()
    object Connecting : BluetoothState()
    data class Connected(val device: BluetoothDevice) : BluetoothState()
    object Disconnecting : BluetoothState()
    object Disconnected : BluetoothState()
}

sealed class BluetoothError {
    object BluetoothNotAvailable : BluetoothError()
    object BluetoothNotEnabled : BluetoothError()
    object LocationPermissionDenied : BluetoothError()
    object BluetoothPermissionDenied : BluetoothError()
    data class ScanFailed(val message: String) : BluetoothError()
    data class ConnectionFailed(val message: String) : BluetoothError()
    data class ConnectionLost(val message: String) : BluetoothError()
    data class Unknown(val message: String) : BluetoothError()
}

sealed class BluetoothResult<out T> {
    data class Success<T>(val data: T) : BluetoothResult<T>()
    data class Error(val error: BluetoothError) : BluetoothResult<Nothing>()
}
