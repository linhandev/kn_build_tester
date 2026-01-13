package com.example.switchlib

fun processValue(): String {
    val value = 3
    return when (value) {
        1 -> "One"
        2 -> "Two"
        3 -> "Three"
        3 -> "Three again"
        5 -> "Five"
        else -> "Other"
    }
}

fun processValueCond(): String {
    val value = 3
    return when {
        value == 1 -> "One"
        value == 2 -> "Two"
        value == 3 -> "Three"
        value == 3 -> "Three again"
        value == 5 -> "Five"
        else -> "Other"
    }
}
