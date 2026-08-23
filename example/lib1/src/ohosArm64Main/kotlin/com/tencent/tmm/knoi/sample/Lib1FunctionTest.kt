package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.KNExport
import com.tencent.tmm.knoi.logger.info

@KNExport(name = "testStringReturnStringLib1")
fun testStringFunction(name: String): String {
    info(name)
    return name + "forKMM"
}