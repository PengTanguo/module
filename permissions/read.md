class MainActivity : ComponentActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)

        // 初始化权限管理器
        AndroidPermissionManager.initialize(this)
    }
}


import permissions.*

class MyViewModel {
private val permissionManager = createPermissionManager()

    suspend fun requestCameraAccess() {
        when (val status = permissionManager.requestPermission(Permission.CAMERA)) {
            is PermissionStatus.Granted -> {
                println("相机权限已授予")
            }
            is PermissionStatus.Denied -> {
                println("相机权限被拒绝")
            }
            is PermissionStatus.DeniedForever -> {
                println("相机权限被永久拒绝，请引导用户到设置页面")
                permissionManager.openAppSettings()
            }
            else -> {
                println("其他状态: $status")
            }
        }
    }
    
    suspend fun requestMultiplePermissions() {
        val permissions = listOf(
            Permission.CAMERA,
            Permission.MICROPHONE,
            Permission.LOCATION
        )
        
        val results = permissionManager.requestPermissions(permissions)
        results.forEach { (permission, status) ->
            println("$permission: $status")
        }
    }
    
    suspend fun checkPermissionStatus() {
        val status = permissionManager.checkPermission(Permission.CAMERA)
        println("相机权限状态: $status")
    }
}
