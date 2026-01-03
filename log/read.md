// Application 中初始化
class MyApplication : Application() {
override fun onCreate() {
super.onCreate()

        Logger.init(
            LogConfig(
                isEnabled = BuildConfig.DEBUG,
                minLevel = LogLevel.DEBUG,
                enableFileLog = true,
                logFilePath = "${getExternalFilesDir(null)?.absolutePath}/logs",
                maxFileSize = 5 * 1024 * 1024, // 5MB
                maxFileCount = 5,
                showStackTrace = true,
                formatOutput = true,
                defaultTag = "MyApp"
            )
        )
    }
}

// 使用
Logger.d("MainActivity", "Activity created")
Logger.e("Network", "Request failed", exception)

// 参数日志
Logger.log(
"userId" to 12345,
"userName" to "John",
"isLogin" to true
)

// 堆栈跟踪
Logger.logTrace(maxDepth = 10, skipSystem = true)

// 获取日志文件
val logFiles = Logger.getLogFiles()

// 清理旧日志
Logger.cleanOldLogs()



// AppDelegate 中初始化
func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

    Logger.shared.init(config: LogConfig(
        isEnabled: true,
        minLevel: .debug,
        enableFileLog: true,
        logFilePath: nil, // 使用默认路径
        maxFileSize: 5 * 1024 * 1024,
        maxFileCount: 5,
        showStackTrace: true,
        formatOutput: true,
        defaultTag: "MyApp"
    ))
    
    return true
}

// 使用
Logger.shared.d(tag: "ViewController", message: "View loaded")
Logger.shared.e(tag: "Network", message: "Request failed", throwable: error)

// 参数日志
Logger.shared.log(
KotlinPair(first: "userId", second: 12345),
KotlinPair(first: "userName", second: "John")
)

// 堆栈跟踪
Logger.shared.logTrace(maxDepth: 10, skipSystem: true)
