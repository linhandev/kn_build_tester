package io.github.kotlin.fibonacci

import kotlin.test.Test
import kotlin.test.assertEquals

class FibiTest {

    @Test
    fun testThirdElement() {
        assertEquals(firstElement + secondElement, generateFibi().take(3).last())
    }
}