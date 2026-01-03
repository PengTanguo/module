package jp.john.sp

// commonMain/kotlin/KMPStorage.kt
interface KMPStorage {
    /**
     * 保存字符串
     */
    fun putString(key: String, value: String)

    /**
     * 获取字符串
     */
    fun getString(key: String, defaultValue: String = ""): String

    /**
     * 保存整数
     */
    fun putInt(key: String, value: Int)

    /**
     * 获取整数
     */
    fun getInt(key: String, defaultValue: Int = 0): Int

    /**
     * 保存长整数
     */
    fun putLong(key: String, value: Long)

    /**
     * 获取长整数
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long

    /**
     * 保存浮点数
     */
    fun putFloat(key: String, value: Float)

    /**
     * 获取浮点数
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float

    /**
     * 保存布尔值
     */
    fun putBoolean(key: String, value: Boolean)

    /**
     * 获取布尔值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    /**
     * 删除指定键
     */
    fun remove(key: String)

    /**
     * 清空所有数据
     */
    fun clear()

    /**
     * 检查键是否存在
     */
    fun contains(key: String): Boolean

    /**
     * 获取所有键
     */
    fun getAllKeys(): Set<String>
}

/**
 * 平台相关的创建函数
 */
expect fun createKMPStorage(name: String = "kmp_storage"): KMPStorage
