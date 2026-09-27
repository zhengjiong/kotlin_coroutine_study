package com.zj.renwuxian

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * CreateTime:2026/9/5 10:38
 * @author zhengjiong
 */

fun main() {
    val c = Coroutine_19_2_15()
//    c.test1()
    c.test2()
}

class Coroutine_19_2_15 {

    fun test1() {
        runBlocking {
            val scope = CoroutineScope(Dispatchers.IO)
            val job = scope.launch {

                this.launch {
                    try {
                        delay(2000)
                    } catch (e: Exception) {
                        //catch child launch e:kotlinx.coroutines.JobCancellationException: Job was cancelled; job=JobImpl{Cancelling}@421819fa
                        println("catch child launch e:$e")
                    }
                }
                try {
                    delay(2000)
                } catch (e: Exception) {
                    //catch launch e:kotlinx.coroutines.JobCancellationException: Job was cancelled; job=JobImpl{Cancelling}@421819fa
                    println("catch launch e:$e")
                }
            }
            delay(500)
            scope.cancel()
            job.join()

            //runBlocking end
            println("runBlocking end")
        }
    }

    fun test2() {
        runBlocking {
            val scope = CoroutineScope(Dispatchers.IO)
            val job = scope.launch {

                this.launch {
                    while (true) {
                        if (!isActive) {
                            println("child isActive=false")
                            throw CancellationException()
                        }
                    }
                }
                try {
                    delay(2000)
                } catch (e: CancellationException) {
                    //catch launch e:kotlinx.coroutines.JobCancellationException: Job was cancelled; job=JobImpl{Cancelling}@6bf3fbb4
                    println("catch launch e:$e")
                    throw e
                }
            }
            delay(500)
            scope.cancel()
            job.join()
            //runBlocking end
            println("runBlocking end")
        }
    }

}