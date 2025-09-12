package stringlib

expect fun getStringPlatform(): String

fun printStringPlatformInfo() {
    println("String platform: ${getStringPlatform()}")
}
