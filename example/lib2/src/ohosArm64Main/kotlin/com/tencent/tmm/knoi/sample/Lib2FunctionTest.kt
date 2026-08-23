@file:Suppress("UNCHECKED_CAST")

package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.*
import com.tencent.tmm.knoi.annotation.KNExport
import com.tencent.tmm.knoi.logger.info
import platform.ohos.knoi.*

@KNExport(name = "testStringReturnStringLib2")
fun testStringFunction(name: String): String {
    info(name)
    return name + "forKMM"
}