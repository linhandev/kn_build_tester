package com.example.kmpmultiplatform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform