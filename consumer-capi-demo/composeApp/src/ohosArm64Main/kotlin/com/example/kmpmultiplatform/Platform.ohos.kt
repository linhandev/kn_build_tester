package com.example.kmpmultiplatform

private class OhosPlatform : Platform {
    override val name: String = "HarmonyOS"
}

actual fun getPlatform(): Platform = OhosPlatform()
