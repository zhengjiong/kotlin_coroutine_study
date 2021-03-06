package com.zj.kotlin.jvm.shengshiyuan

import com.zj.kotlin.jvm.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread


fun main() {
    val demo = HelloCoroutine1()
    //demo.test1()
    //demo.test2()
    //demo.test3()
    demo.test4()
}

/**
 * GlobalScope.launch{}是最常用的Coroutine builders, 它不会
 * 阻塞当前线程, 会在后台创建一个新的协程,也可以指定协程调度器.
 */
class HelloCoroutine1 {

    /**
     * Thread[main,5,main] -> Hello,
     * Thread[DefaultDispatcher-worker-1,5,main] -> world
     */
    fun test1() {
        // 在 GlobalScope 中启动的活动协程并不会使进程保活。它们就像守护线程。
        // 在后台启动一个新的协程并继续
        GlobalScope.launch {
            //delay 是一个特殊的 挂起函数 ，它不会造成线程阻塞，但是会 挂起 协程，并且只能在协程中使用。
            delay(1000)// 非阻塞的等待 1 秒钟（默认时间单位是毫秒）
            Logger.i("world")// 在延迟后打印输出
        }

        Logger.i("Hello,")// 协程已在等待时主线程还在继续
        Thread.sleep(2000)// 阻塞主线程 2 秒钟来保证 JVM 存活
    }

    /**
     * Thread[main,5,main] -> Hello
     * Thread[main,5,main] -> World
     * Thread[Thread-0,5,main] -> Kotlin Coroutines
     *
     */
    fun test2() {
        thread {
            Thread.sleep(1000)
            Logger.i("Kotlin Coroutines")
        }

        /*Thread(Runnable{
            Thread.sleep(1000)
            Logger.i("Kotlin Coroutines")
        }).start()*/

        Logger.i("Hello")

        //Thread.sleep(2000)

        Logger.i("World")
    }

    fun test3() {
        /**
         * 开启多个线程在内存比较少的情况下会OutOfMemoryError出错,
         * 但是用协程就不会
         */
        repeat(100000) {
            thread {
                Thread.sleep(1000)
                println(it)
            }
        }
        println("end")
    }

    /**
     * Thread[main,5,main] -> start
     * Thread[main,5,main] -> 1
     * Thread[main,5,main] -> 2
     * Thread[main,5,main] -> delay
     * Thread[main,5,main] -> delay
     * Thread[main,5,main] -> delay
     * Thread[main,5,main] -> 0
     * Thread[main,5,main] -> 1
     * Thread[main,5,main] -> 2
     * Thread[main,5,main] -> end
     */
    fun test4() {
        Logger.i("start")
        runBlocking {
            Logger.i(1)
            delay(2000)
            Logger.i(2)
            repeat(3) { index ->
                launch {
                    Logger.i("delay")
                    delay(1000)
                    Logger.i(index)
                }
            }
        }
        Logger.i("end")
    }
}