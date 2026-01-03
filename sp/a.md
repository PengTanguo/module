// 在 Application 中初始化
class MyApplication : Application() {
override fun onCreate() {
super.onCreate()
KMPStorageInitializer.init(this)
}
}

// 使用
val storage = createKMPStorage("my_app_prefs")
storage.putString("username", "John")
val username = storage.getString("username")



// 直接使用
let storage = KMPStorageKt.createKMPStorage(name: "my_app_prefs")
storage.putString(key: "username", value: "John")
let username = storage.getString(key: "username", defaultValue: "")
