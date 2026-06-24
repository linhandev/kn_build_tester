package io.github.kotlin.fibonacci

import kotlin.test.Test
import kotlin.test.assertEquals

class JsFibiTest {

    @Test
    fun testThirdElement() {
        assertEquals(firstElement + secondElement, generateFibi().take(3).last())
    }
}
