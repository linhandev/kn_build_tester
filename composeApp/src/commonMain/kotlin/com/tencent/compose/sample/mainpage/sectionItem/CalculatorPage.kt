package com.tencent.compose.sample.mainpage.sectionItem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.compose.sample.calculator.Calculator

@Composable
fun CalculatorPage() {
    val calculator = Calculator()
    val a = 10.0
    val b = 5.0

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Calculator Demo")
        Text("Values: a = $a, b = $b")
        Text("Add: ${calculator.add(a, b)}")
        Text("Subtract: ${calculator.subtract(a, b)}")
        Text("Multiply: ${calculator.multiply(a, b)}")
        Text("Divide: ${calculator.divide(a, b)}")
    }
}
