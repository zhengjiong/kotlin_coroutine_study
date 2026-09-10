package com.zj.renwuxian

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import java.lang.Exception
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 对应视频: 030.3.4-coroutine Scope() 和 superv.mp4
 * CreateTime:2026/9/10 15:02
 * @author zhengjiong
 */

fun main() {
    val demo = SupervisorJob_30_3_4()
//    demo.test1()
//    demo.test2()
    demo.test3()
}

class SupervisorJob_30_3_4 {

    fun test1() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        val job = scope.launch {
            //这里可以对coroutineScope进行try-catch
            coroutineScope {
                launch {
                    delay(500)
                    throw RuntimeException("error")
                }
            }
            try {
                delay(2000)
            } catch (e: Exception) {
                //不会输出,因为coroutineScope是挂起函数,根本不会执行到delay这里
                println("job catch CancellationException $e")
            }
        }
        delay(1000)
        //job is cancel true
        println("job is cancel ${job.isCancelled}")
        //scope is cancel true
        println("scope is cancel ${scope.coroutineContext.job.isCancelled}")
        delay(3000)
    }

    fun test2() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        val job = scope.launch {
            supervisorScope {
                launch {
                    throw RuntimeException("error")
                }
            }
            try {
                delay(2000)
            } catch (e: CancellationException) {
                //不会输出,因为supervisorScope是挂起函数,根本不会执行到delay这里
                println("job catch CancellationException")
            }
        }
        delay(1000)
        //job is cancel false
        println("job is cancel ${job.isCancelled}")
        //scope is cancel false
        println("scope is cancel ${scope.coroutineContext.job.isCancelled}")
        delay(3000)
    }

    fun test3() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        var childJob1 : Job? = null
        var childJob2 : Job? = null
        val job = scope.launch {
            supervisorScope {
                childJob1 = launch {
                    throw RuntimeException("error")
                }

                childJob2 = launch {
                    try {
                        delay(3000)
                    } catch (e: Exception) {
                        println("child2 catch CancellationException")
                    }
                }
            }
            try {
                delay(2000)
            } catch (e: CancellationException) {
                //不会输出,因为supervisorScope是挂起函数,根本不会执行到delay这里
                println("job catch CancellationException")
            }
            //会输出:job done
            println("job done")
        }
        delay(1000)
        //job is cancel false
        println("job is cancel ${job.isCancelled}")
        //scope is cancel false
        println("scope is cancel ${scope.coroutineContext.job.isCancelled}")

        //因为使用了supervisorScope,所以childJob1不会被影响
        //childJob1 is cancel true
        println("childJob1 is cancel ${childJob1?.isCancelled}")
        //childJob2 is cancel false
        println("childJob2 is cancel ${childJob2?.isCancelled}")
        delay(5000)
    }

    fun test4() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        var childJob1 : Job? = null
        var childJob2 : Job? = null
        val job = scope.launch {
            coroutineScope {
                childJob1 = launch {
                    throw RuntimeException("error")
                }

                childJob2 = launch {
                    try {
                        delay(3000)
                    } catch (e: Exception) {
                        println("child2 catch CancellationException")
                    }
                }
            }
            try {
                delay(2000)
            } catch (e: CancellationException) {
                //不会输出,因为supervisorScope是挂起函数,根本不会执行到delay这里
                println("job catch CancellationException")
            }
            //会输出:job done
            println("job done")
        }
        delay(1000)
        //job is cancel false
        println("job is cancel ${job.isCancelled}")
        //scope is cancel false
        println("scope is cancel ${scope.coroutineContext.job.isCancelled}")

        //因为使用了coroutineScope,所以childJob1收到了影响
        //childJob1 is cancel false
        println("childJob1 is cancel ${childJob1?.isCancelled}")
        //childJob2 is cancel false
        println("childJob2 is cancel ${childJob2?.isCancelled}")
        delay(5000)
    }
}