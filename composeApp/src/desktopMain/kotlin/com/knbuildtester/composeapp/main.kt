package com.knbuildtester.composeapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KN Build Tester",
    ) {
        App()
    }
}