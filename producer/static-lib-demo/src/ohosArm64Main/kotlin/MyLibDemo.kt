@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

import demo.mylib.mylib_add
import demo.mylib.mylib_answer

@CName("kn_staticlib_demo")
fun staticLibDemo(): Int {
    // mylib_add/mylib_answer come from the .a embedded in the klib via staticLibraries.
    // Consumer links the .a automatically (KGP handles included .a), no -L/-l needed.
    return mylib_add(mylib_answer(), mylib_answer())  // 42 + 42 = 84
}
