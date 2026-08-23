package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.KNExport

@KNExport
const val testString: String = "testStringDeclare"

@KNExport("testStringDeclareCustomName")
const val customName: String = "testStringDeclare"

class DeclareTest {
    companion object {
        @KNExport
        const val testString: String = "testStringDeclareInClass"
    }
}