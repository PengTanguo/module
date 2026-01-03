package jp.john.sp

// iosMain/kotlin/KMPStorage.ios.kt
import platform.Foundation.*

actual fun createKMPStorage(name: String): KMPStorage {
    return IosKMPStorage(name)
}

class IosKMPStorage(private val name: String) : KMPStorage {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    private fun prefixedKey(key: String) = "${name}_$key"

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, prefixedKey(key))
        userDefaults.synchronize()
    }

    override fun getString(key: String, defaultValue: String): String {
        return userDefaults.stringForKey(prefixedKey(key)) ?: defaultValue
    }

    override fun putInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), prefixedKey(key))
        userDefaults.synchronize()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return if (userDefaults.objectForKey(prefixedKey(key)) != null) {
            userDefaults.integerForKey(prefixedKey(key)).toInt()
        } else {
            defaultValue
        }
    }

    override fun putLong(key: String, value: Long) {
        userDefaults.setObject(value, prefixedKey(key))
        userDefaults.synchronize()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return (userDefaults.objectForKey(prefixedKey(key)) as? Long) ?: defaultValue
    }

    override fun putFloat(key: String, value: Float) {
        userDefaults.setFloat(value, prefixedKey(key))
        userDefaults.synchronize()
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return if (userDefaults.objectForKey(prefixedKey(key)) != null) {
            userDefaults.floatForKey(prefixedKey(key))
        } else {
            defaultValue
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, prefixedKey(key))
        userDefaults.synchronize()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (userDefaults.objectForKey(prefixedKey(key)) != null) {
            userDefaults.boolForKey(prefixedKey(key))
        } else {
            defaultValue
        }
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(prefixedKey(key))
        userDefaults.synchronize()
    }

    override fun clear() {
        val keys = getAllKeys()
        keys.forEach { key ->
            userDefaults.removeObjectForKey(prefixedKey(key))
        }
        userDefaults.synchronize()
    }

    override fun contains(key: String): Boolean {
        return userDefaults.objectForKey(prefixedKey(key)) != null
    }

    override fun getAllKeys(): Set<String> {
        val dictionary = userDefaults.dictionaryRepresentation()
        val prefix = "${name}_"
        return dictionary.keys
            .filterIsInstance<String>()
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
            .toSet()
    }
}
