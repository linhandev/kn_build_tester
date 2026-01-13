package com.example.switchlib

import kotlin.random.Random

fun processValue(): String {
//    val value = Random.nextInt(6)
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

fun processValueNoNewLine(): String {
//    val value = Random.nextInt(6)
    val value = 3
    return when (value) {
        1 -> "One"
        2 -> "Two"
        3 -> "Three"
        5 -> "Five"
        else -> "Other"
    }
}
