package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.KNExport

@KNExport
const val testStringLib2: String = "testStringDeclare"

class DeclareTestLib2 {
    companion object {
        @KNExport
        const val testString: String = "testStringDeclareInClass"
    }
}