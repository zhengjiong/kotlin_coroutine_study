package com.zj.kotlin.jvm

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


fun main() {
    val demo = HelloCoroutine1()
    demo.test1()
}

class HelloCoroutine1 {

    fun test1() {
        GlobalScope.launch {
            delay(1000)
        }
    }
}