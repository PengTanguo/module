package com.anker.myapplication

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform