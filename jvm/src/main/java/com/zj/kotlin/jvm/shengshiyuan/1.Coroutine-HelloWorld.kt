package com.zj.kotlin.jvm.shengshiyuan

import com.zj.kotlin.jvm.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread


fun main() {
    val demo = HelloCoroutine1()
    //demo.test1()
    demo.test2()
}

class HelloCoroutine1 {

    /**
     * Thread[main,5,main] -> Hello,
     * Thread[DefaultDispatcher-worker-1,5,main] -> world
     */
    fun test1() {
        // 在后台启动一个新的协程并继续
        GlobalScope.launch {
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
}