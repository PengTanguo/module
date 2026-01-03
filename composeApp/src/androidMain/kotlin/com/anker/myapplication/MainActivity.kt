package com.anker.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import jp.john.log.LogConfig
import jp.john.log.LogLevel
import jp.john.log.Logger
import jp.john.sp.KMPStorage
import jp.john.sp.KMPStorageInitializer
import jp.john.sp.createKMPStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Logger.init(
            LogConfig(
                isEnabled = true,
                minLevel = LogLevel.DEBUG,
                enableFileLog = true,
                logFilePath = "${getExternalFilesDir(null)?.absolutePath}/logs",
                maxFileSize = 5 *1024 *1024, // 5MB
                maxFileCount = 5,
                showStackTrace = true,
                formatOutput = true,
                defaultTag = "MyApp"
            )
        )
        Logger.d("jasonjohn","this is test")
        KMPStorageInitializer.init(this,"myapp")
        Logger.d("jasonjohn","this is test"+ KMPStorageInitializer.getSp().getString("jason"))
        KMPStorageInitializer.getSp().putString("jason","adadsd")


        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}