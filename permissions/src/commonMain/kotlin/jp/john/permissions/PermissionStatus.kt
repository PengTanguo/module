package jp.john.permissions


/**
 * 权限状态
 */
sealed class PermissionStatus {
    object Granted : PermissionStatus()
    object Denied : PermissionStatus()
    object DeniedForever : PermissionStatus() // 用户选择了"不再询问"
    object NotDetermined : PermissionStatus() // 未请求过
    object Restricted : PermissionStatus() // iOS专用：受限制
}
