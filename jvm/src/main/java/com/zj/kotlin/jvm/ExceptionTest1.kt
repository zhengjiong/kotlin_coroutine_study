package com.zj.kotlin.jvm

import kotlinx.coroutines.*

/**
 *
 * CreateTime:2022/5/25 09:10
 * @author zhengjiong
 */

fun main() {
    val obj = ExceptionTest1()
    obj.test1()
    //obj.test2()
    //obj.test3()
}

class ExceptionTest1 {

    //try不到
    fun test1() = runBlocking {
        launch {
            try {
                async {
                    //可以再内部增加try-catch
                    delay(3000)
                    throw RuntimeException("zhengjiong")
                }.await()
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    //可以try
    fun test2() = runBlocking {
        launch {
            try {
                coroutineScope {
                    async {
                        delay(3000)
                        throw RuntimeException("zhengjiong")
                    }.await()
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    //可以try
    fun test3() = runBlocking {
        launch {
            try {
                supervisorScope {
                    async {
                        delay(3000)
                        throw RuntimeException("zhengjiong")
                    }.await()
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }
}