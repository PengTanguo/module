package jp.john.sp

// androidMain/kotlin/KMPStorage.android.kt
import android.content.Context
import android.content.SharedPreferences

actual fun createKMPStorage(name: String): KMPStorage {
    return AndroidKMPStorage(name)
}

class AndroidKMPStorage(name: String) : KMPStorage {

    private val sharedPreferences: SharedPreferences by lazy {
        val context = KMPStorageInitializer.applicationContext
            ?: throw IllegalStateException("KMPStorage must be initialized with Application Context")
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    override fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    override fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    override fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    override fun putFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    override fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    override fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    override fun getAllKeys(): Set<String> {
        return sharedPreferences.all.keys
    }
}

/**
 * Android 初始化器
 */
object KMPStorageInitializer {
    var applicationContext: Context? = null
        private set
    private var defaultStorage: KMPStorage? = null

    fun init(context: Context,name: String) {
        applicationContext = context.applicationContext
        defaultStorage=createKMPStorage(name)
    }

    fun  getSp():KMPStorage{
        return defaultStorage!!
    }


}
