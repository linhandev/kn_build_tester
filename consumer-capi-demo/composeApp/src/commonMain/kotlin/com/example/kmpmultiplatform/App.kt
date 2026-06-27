package com.example.kmpmultiplatform

data class AppText(
    val text: String,
    val colorHex: String,
    val sizeSp: Float,
    val bold: Boolean
)

fun App(): AppText = AppText(
    text = "Hello World",
    colorHex = "#181818",
    sizeSp = 32f,
    bold = true
)
