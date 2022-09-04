package com.zj.kotlin.coroutine

import kotlin.coroutines.*

/**
 *
 * CreateTime:2022/6/26 16:42
 * @author zhengjiong
 */

fun main() {
    val coroutine = suspend {
        println("in coroutine.")
        5
    }.createCoroutine(object : Continuation<Int> {
        override fun resumeWith(result: Result<Int>) {
            println("resumeWith result=$result")
        }

        override val context: CoroutineContext
            get() = EmptyCoroutineContext
    })

    coroutine.resume(Unit)
}

class CoroutineBasic {

}