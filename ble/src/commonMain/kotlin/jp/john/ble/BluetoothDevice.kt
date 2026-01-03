package jp.john.ble


data class BluetoothDevice(
    val id: String,
    val name: String?,
    val rssi: Int,
    val isConnectable: Boolean = true,
    val manufacturerData: Map<String, ByteArray>? = null
)
