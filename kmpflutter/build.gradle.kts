plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    kotlin("plugin.serialization") version "2.1.0" // 添加序列化插件
    id("kotlin-parcelize")

}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "jp.john.kmpflutter"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "kmpflutterKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
            isStatic = true // 重要：设置为静态框架

        }
    }
    iosArm64 {
        binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // 协程支持 - 用于异步操作
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // JSON 序列化 - 用于数据传输
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // 日期时间
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                // Add KMP dependencies here
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Android 核心库
                implementation("androidx.core:core-ktx:1.13.1")

                // 协程 Android 支持
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

                // Lifecycle (可选 - 用于生命周期感知)
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }

}