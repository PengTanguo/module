package jp.john.permissions


import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.Contacts.*
import platform.CoreLocation.*
import platform.EventKit.*
import platform.Photos.*
import platform.UIKit.*
import platform.UserNotifications.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createPermissionManager(): PermissionManager {
    return IOSPermissionManager()
}

class IOSPermissionManager : PermissionManager {

    private val locationManager = CLLocationManager()

    override suspend fun checkPermission(permission: Permission): PermissionStatus {
        return when (permission) {
            Permission.CAMERA -> checkCameraPermission()
            Permission.GALLERY -> checkPhotoLibraryPermission()
            Permission.LOCATION -> checkLocationPermission()
            Permission.LOCATION_ALWAYS -> checkLocationAlwaysPermission()
            Permission.MICROPHONE -> checkMicrophonePermission()
            Permission.NOTIFICATIONS -> checkNotificationPermission()
            Permission.CONTACTS -> checkContactsPermission()
            Permission.CALENDAR -> checkCalendarPermission()
            Permission.STORAGE -> checkPhotoLibraryPermission()
            Permission.BLUETOOTH -> checkBluetoothPermission()
        }
    }

    override suspend fun requestPermission(permission: Permission): PermissionStatus {
        return when (permission) {
            Permission.CAMERA -> requestCameraPermission()
            Permission.GALLERY -> requestPhotoLibraryPermission()
            Permission.LOCATION -> requestLocationPermission()
            Permission.LOCATION_ALWAYS -> requestLocationAlwaysPermission()
            Permission.MICROPHONE -> requestMicrophonePermission()
            Permission.NOTIFICATIONS -> requestNotificationPermission()
            Permission.CONTACTS -> requestContactsPermission()
            Permission.CALENDAR -> requestCalendarPermission()
            Permission.STORAGE -> requestPhotoLibraryPermission()
            Permission.BLUETOOTH -> PermissionStatus.Granted // iOS蓝牙不需要显式请求
        }
    }

    override suspend fun requestPermissions(permissions: List<Permission>): Map<Permission, PermissionStatus> {
        val result = mutableMapOf<Permission, PermissionStatus>()
        permissions.forEach { permission ->
            result[permission] = requestPermission(permission)
        }
        return result
    }

    override fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        settingsUrl?.let {
            if (UIApplication.sharedApplication.canOpenURL(it)) {
                UIApplication.sharedApplication.openURL(it)
            }
        }
    }

    // Camera
    private fun checkCameraPermission(): PermissionStatus {
        return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> PermissionStatus.Granted
            AVAuthorizationStatusDenied -> PermissionStatus.Denied
            AVAuthorizationStatusRestricted -> PermissionStatus.Restricted
            AVAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
            else -> PermissionStatus.NotDetermined
        }
    }

    private suspend fun requestCameraPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                continuation.resume(
                    if (granted) PermissionStatus.Granted else PermissionStatus.Denied
                )
            }
        }
    }

    // Photo Library
    private fun checkPhotoLibraryPermission(): PermissionStatus {
        return when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> PermissionStatus.Granted
            PHAuthorizationStatusDenied -> PermissionStatus.Denied
            PHAuthorizationStatusRestricted -> PermissionStatus.Restricted
            PHAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
            else -> PermissionStatus.NotDetermined
        }
    }

    private suspend fun requestPhotoLibraryPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorization { status ->
                continuation.resume(
                    when (status) {
                        PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited ->
                            PermissionStatus.Granted
                        PHAuthorizationStatusDenied -> PermissionStatus.Denied
                        PHAuthorizationStatusRestricted -> PermissionStatus.Restricted
                        else -> PermissionStatus.NotDetermined
                    }
                )
            }
        }
    }

    // Location
    private fun checkLocationPermission(): PermissionStatus {
        return when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> PermissionStatus.Granted
            kCLAuthorizationStatusDenied -> PermissionStatus.Denied
            kCLAuthorizationStatusRestricted -> PermissionStatus.Restricted
            kCLAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
            else -> PermissionStatus.NotDetermined
        }
    }

    private suspend fun requestLocationPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            locationManager.requestWhenInUseAuthorization()
            // 注意：实际项目中需要实现 CLLocationManagerDelegate 来获取回调
            continuation.resume(checkLocationPermission())
        }
    }

    private fun checkLocationAlwaysPermission(): PermissionStatus {
        return when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedAlways -> PermissionStatus.Granted
            kCLAuthorizationStatusDenied -> PermissionStatus.Denied
            kCLAuthorizationStatusRestricted -> PermissionStatus.Restricted
            else -> PermissionStatus.NotDetermined
        }
    }

    private suspend fun requestLocationAlwaysPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            locationManager.requestAlwaysAuthorization()
            continuation.resume(checkLocationAlwaysPermission())
        }
    }

    // Microphone
    private fun checkMicrophonePermission(): PermissionStatus {
        return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio)) {
            AVAuthorizationStatusAuthorized -> PermissionStatus.Granted
            AVAuthorizationStatusDenied -> PermissionStatus.Denied
            AVAuthorizationStatusRestricted -> PermissionStatus.Restricted
            AVAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
            else -> PermissionStatus.NotDetermined
        }
    }

    private suspend fun requestMicrophonePermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { granted ->
                continuation.resume(
                    if (granted) PermissionStatus.Granted else PermissionStatus.Denied
                )
            }
        }
    }

    // Notifications
    private suspend fun checkNotificationPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    continuation.resume(
                        when (settings?.authorizationStatus) {
                            UNAuthorizationStatusAuthorized -> PermissionStatus.Granted
                            UNAuthorizationStatusDenied -> PermissionStatus.Denied
                            UNAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
                            else -> PermissionStatus.NotDetermined
                        }
                    )
                }
        }
    }

    private suspend fun requestNotificationPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptionAlert or
                    UNAuthorizationOptionSound or
                    UNAuthorizationOptionBadge

            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(options) { granted, error ->
                    continuation.resume(
                        if (granted) PermissionStatus.Granted else PermissionStatus.Denied
                    )
                }
        }
    }

    // Contacts
    private fun checkContactsPermission(): PermissionStatus {
        return when (CNContactStore.authorizationStatusForEntityType(CNEntityTypeContacts)) {
            CNAuthorizationStatusAuthorized -> PermissionStatus.Granted
            CNAuthorizationStatusDenied -> PermissionStatus.Denied
            CNAuthorizationStatusRestricted -> PermissionStatus.Restricted
            CNAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
            else -> PermissionStatus.NotDetermined
        }
    }

    private suspend fun requestContactsPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            CNContactStore().requestAccessForEntityType(CNEntityTypeContacts) { granted, error ->
                continuation.resume(
                    if (granted) PermissionStatus.Granted else PermissionStatus.Denied
                )
            }
        }
    }

    // Calendar
    private fun checkCalendarPermission(): PermissionStatus {
        return when (EKEventStore.authorizationStatusForEntityType(EKEntityTypeEvent)) {
            EKAuthorizationStatusAuthorized -> PermissionStatus.Granted
            EKAuthorizationStatusDenied -> PermissionStatus.Denied
            EKAuthorizationStatusRestricted -> PermissionStatus.Restricted
            EKAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
            else -> PermissionStatus.NotDetermined
        }
    }

    private suspend fun requestCalendarPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            EKEventStore().requestAccessToEntityType(EKEntityTypeEvent) { granted, error ->
                continuation.resume(
                    if (granted) PermissionStatus.Granted else PermissionStatus.Denied
                )
            }
        }
    }

    // Bluetooth
    private fun checkBluetoothPermission(): PermissionStatus {
        // iOS 蓝牙权限通常不需要显式请求
        return PermissionStatus.Granted
    }
}
