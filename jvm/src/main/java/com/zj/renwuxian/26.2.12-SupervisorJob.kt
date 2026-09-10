package com.zj.renwuxian

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 对应视频: 026.2.12-Supervisor Job.mp4
 * CreateTime:2026/9/9 18:02
 * @author zhengjiong
 */

fun main() {
    val demo = SupervisorJob_26_2_12()
    demo.test1()
//    demo.test1_1()
    //demo.test1_1_1()
//    demo.test1_1_2()
//    demo.test1_2()
//    demo.test2()
    //demo.test3()
    //demo.test4()
//    demo.test5()
}

class SupervisorJob_26_2_12 {
    fun test1() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        var childJob : Job? = null
        val job = scope.launch {
            childJob = launch(SupervisorJob(coroutineContext.job)) {
                throw RuntimeException("error!")
            }
            delay(3000)
        }
        delay(1000)
        //Parent Job canceled: false
        println("Parent Job canceled: ${job.isCancelled}")

        //childJob canceled: true
        println("childJob canceled: ${childJob?.isCancelled}")
        delay(3000)
    }

    fun test1_1() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        val job = scope.launch {
            launch(SupervisorJob()) {
                throw RuntimeException("error!")
            }
            try {
                delay(3000)
            } catch (e: CancellationException) {
                //不会输出
                println("parent catch cancel")
            }
        }
        delay(1000)
        //Parent Job canceled: false
        println("Parent Job canceled: ${job.isCancelled}")
        //scope Job canceled: false, true
        println("scope Job canceled: ${scope.coroutineContext.job.isCancelled}, ${scope.isActive}")
        delay(3000)
    }

    fun test1_1_1() = runBlocking {
        val scope = CoroutineScope(SupervisorJob())

        val job = scope.launch {
            launch() {
                throw RuntimeException("error!")
            }
            try {
                delay(3000)
            } catch (e: CancellationException) {
                //会输出
                println("parent catch cancel")
            }
        }
        delay(1000)
        //Parent Job canceled: true
        println("Parent Job canceled: ${job.isCancelled}")
        //scope Job canceled: false, true
        println("scope Job canceled: ${scope.coroutineContext.job.isCancelled}, ${scope.isActive}")
        delay(3000)
    }

    fun test1_1_2() = runBlocking {
        val scope = CoroutineScope(SupervisorJob())

        val job1 = scope.launch {
            throw RuntimeException("error!")
        }

        val job2 = scope.launch {
            try {
                delay(3000)
            } catch (e: CancellationException) {
                //不会输出
                println("job2 catch cancel $e")
            }
        }
        delay(1000)
        //Job1 canceled: true
        println("Job1 canceled: ${job1.isCancelled}")
        //Job2 canceled: false
        println("Job2 canceled: ${job2.isCancelled}")
        //scope Job canceled: false, true
        println("scope Job canceled: ${scope.coroutineContext.job.isCancelled}, ${scope.isActive}")
        delay(3000)
    }

    fun test1_1_3() = runBlocking {
        val scope = CoroutineScope(SupervisorJob())

        val job1 = scope.launch {
            launch() {
                throw RuntimeException("error!")
            }
            try {
                delay(3000)
            } catch (e: CancellationException) {
                //会输出
                println("job1 catch cancel $e")
            }
        }

        val job2 = scope.launch {
            try {
                delay(3000)
            } catch (e: CancellationException) {
                //不会输出
                println("job2 catch cancel $e")
            }
        }
        delay(1000)
        //Job1 canceled: true
        println("Job1 canceled: ${job1.isCancelled}")
        //Job2 canceled: false
        println("Job2 canceled: ${job2.isCancelled}")
        //scope Job canceled: false, true
        println("scope Job canceled: ${scope.coroutineContext.job.isCancelled}, ${scope.isActive}")
        delay(3000)
    }

    fun test1_2() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        var childJob : Job? = null
        val job = scope.launch {
            childJob = launch(SupervisorJob()) {
                delay(2000)
            }
            delay(3000)
        }
        delay(1000)
        job.cancel()
        //Parent Job canceled: true
        println("Parent Job canceled: ${job.isCancelled}")

        //childJob Job canceled: false
        println("childJob Job canceled: ${childJob?.isCancelled}")
        delay(3000)
    }

    fun test1_3() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        var childJob : Job? = null
        val job = scope.launch {
            childJob = launch(SupervisorJob(coroutineContext.job)) {
                delay(2000)
            }
            delay(3000)
        }
        delay(1000)
        job.cancel()
        //Parent Job canceled: true
        println("Parent Job canceled: ${job.isCancelled}")

        //childJob Job canceled: true
        println("childJob Job canceled: ${childJob?.isCancelled}")
        delay(3000)
    }

    fun test2() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            //会输出
            println("test2 exceptionHandler $exception")
        }

        var childJob: Job? = null
        val job = scope.launch {
            childJob = launch(SupervisorJob(coroutineContext.job) + exceptionHandler) {

                //如果把exceptionHandler写在这里是捕获不到的,而写在
                //上面两个地方都能捕获到
                launch {
                    throw RuntimeException("error!")
                }
                delay(3000)
            }
        }
        delay(1000)
        //Parent Job canceled: false
        println("Parent Job canceled: ${job.isCancelled}")

        //childJob Job canceled: true
        println("childJob Job canceled: ${childJob?.isCancelled}")
        delay(3000)
    }

    fun test3() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)

        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("test2 exceptionHandler $exception")
        }

        val job = scope.launch {
            //和test2结果一样,设置在这里或者外层都可以捕获到
            launch(SupervisorJob(coroutineContext.job) + exceptionHandler) {
                throw RuntimeException("error!")
            }
            delay(3000)
        }
        delay(1000)
        //Parent Job canceled: false
        println("Parent Job canceled: ${job.isCancelled}")
        delay(3000)
    }


    fun test4() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val scope2 = CoroutineScope(SupervisorJob())
        val supervisorJob = SupervisorJob()

        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("test2 exceptionHandler $exception")
        }

        val job = scope.launch {
            //async作为最外层的父协程或者是现在这种supervisorJob的直接子Job来使用的话会
            //把异常吞掉,也就是不立马抛出异常而是在await的时候在抛出
            async(SupervisorJob(coroutineContext.job) + exceptionHandler) {
                throw RuntimeException("error!")
            }
        }
        delay(1000)
        println("Parent Job canceled: ${job.isCancelled}")
        delay(3000)
    }

    fun test5() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("test5 exceptionHandler $exception")
            //exceptionHandler Parent Job isActive: true
            println("exceptionHandler Parent Job isActive: ${coroutineContext.isActive}")

            //exceptionHandler Parent Job canceled: false
            println("exceptionHandler Parent Job canceled: ${coroutineContext.job.isCancelled}")
        }
        val job = scope.launch(exceptionHandler) {
            launch(SupervisorJob(coroutineContext.job)) {
                throw RuntimeException("error!")
            }
            println("parent start")
            delay(3000)
            println("parent done")
        }
        delay(1000)
        //Parent Job canceled: false
        println("Parent Job canceled: ${job.isCancelled}")
        //scope Job isActive: true
        println("scope2 Job isActive: ${scope.isActive}")
        delay(3000)
    }
}