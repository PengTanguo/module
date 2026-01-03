package jp.john.permissions


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createPermissionManager(): PermissionManager {
    return AndroidPermissionManager.instance
}

class AndroidPermissionManager private constructor() : PermissionManager {

    companion object {
        val instance = AndroidPermissionManager()

        private lateinit var activity: ComponentActivity

        /**
         * 必须在 Application 或 Activity 中初始化
         */
        fun initialize(activity: ComponentActivity) {
            this.activity = activity
        }
    }

    private val permissionCallbacks = mutableMapOf<Permission, (PermissionStatus) -> Unit>()

    override suspend fun checkPermission(permission: Permission): PermissionStatus {
        val androidPermission = permission.toAndroidPermission()

        return when {
            ContextCompat.checkSelfPermission(
                activity,
                androidPermission
            ) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.Granted
            }
            ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermission) -> {
                PermissionStatus.Denied
            }
            else -> {
                // 首次请求或用户选择了"不再询问"
                val prefs = activity.getSharedPreferences("permissions", Context.MODE_PRIVATE)
                if (prefs.getBoolean(permission.name, false)) {
                    PermissionStatus.DeniedForever
                } else {
                    PermissionStatus.NotDetermined
                }
            }
        }
    }

    override suspend fun requestPermission(permission: Permission): PermissionStatus {
        // 先检查当前状态
        val currentStatus = checkPermission(permission)
        if (currentStatus is PermissionStatus.Granted) {
            return currentStatus
        }

        return suspendCancellableCoroutine { continuation ->
            val launcher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                val status = if (isGranted) {
                    PermissionStatus.Granted
                } else {
                    // 记录已请求过
                    val prefs = activity.getSharedPreferences("permissions", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(permission.name, true).apply()

                    val androidPermission = permission.toAndroidPermission()
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermission)) {
                        PermissionStatus.Denied
                    } else {
                        PermissionStatus.DeniedForever
                    }
                }

                if (continuation.isActive) {
                    continuation.resume(status)
                }
            }

            launcher.launch(permission.toAndroidPermission())
        }
    }

    override suspend fun requestPermissions(permissions: List<Permission>): Map<Permission, PermissionStatus> {
        val result = mutableMapOf<Permission, PermissionStatus>()

        return suspendCancellableCoroutine { continuation ->
            val androidPermissions = permissions.map { it.toAndroidPermission() }.toTypedArray()

            val launcher = activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                permissions.forEach { permission ->
                    val androidPermission = permission.toAndroidPermission()
                    val isGranted = results[androidPermission] ?: false

                    result[permission] = if (isGranted) {
                        PermissionStatus.Granted
                    } else {
                        val prefs = activity.getSharedPreferences("permissions", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean(permission.name, true).apply()

                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermission)) {
                            PermissionStatus.Denied
                        } else {
                            PermissionStatus.DeniedForever
                        }
                    }
                }

                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            launcher.launch(androidPermissions)
        }
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
    }

    private fun Permission.toAndroidPermission(): String {
        return when (this) {
            Permission.CAMERA -> Manifest.permission.CAMERA
            Permission.GALLERY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            }
            Permission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
            Permission.LOCATION_ALWAYS -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            Permission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
            Permission.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    // 低版本默认授予
                    ""
                }
            }
            Permission.CONTACTS -> Manifest.permission.READ_CONTACTS
            Permission.CALENDAR -> Manifest.permission.READ_CALENDAR
            Permission.STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            }
            Permission.BLUETOOTH -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_CONNECT
                } else {
                    Manifest.permission.BLUETOOTH
                }
            }
        }
    }
}
