package jp.john.permissions


/**
 * 权限管理器接口
 */
interface PermissionManager {
    /**
     * 检查权限状态
     */
    suspend fun checkPermission(permission: Permission): PermissionStatus

    /**
     * 请求单个权限
     */
    suspend fun requestPermission(permission: Permission): PermissionStatus

    /**
     * 请求多个权限
     */
    suspend fun requestPermissions(permissions: List<Permission>): Map<Permission, PermissionStatus>

    /**
     * 打开应用设置页面
     */
    fun openAppSettings()

    /**
     * 权限是否已授予
     */
    suspend fun isPermissionGranted(permission: Permission): Boolean {
        return checkPermission(permission) is PermissionStatus.Granted
    }
}

/**
 * 创建平台特定的权限管理器
 */
expect fun createPermissionManager(): PermissionManager
