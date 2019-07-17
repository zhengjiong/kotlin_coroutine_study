package com.zj.kotlin.jvm.shengshiyuan

import com.zj.kotlin.jvm.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 *
 * CreateTime:2019-07-17  17:27
 * @author 郑炯
 * @version 1.0
 */

fun main() {
    val demo = HelloCoroutine2()
    //demo.test1()
    demo.test2()
}

class HelloCoroutine2 {

    /**
     * Thread[main,5,main] -> 1
     * Thread[DefaultDispatcher-worker-1,5,main] -> Kotlin Coroutines
     * Thread[main,5,main] -> 2
     * Thread[main,5,main] -> end
     */
    fun test1() {
        /**
         * 调用了 runBlocking 的主线程会一直 阻塞 直到 runBlocking 内部的协程执行完毕。
         */
        runBlocking {
            GlobalScope.launch {
                //delay 是一个特殊的 挂起函数 ，它不会造成线程阻塞，但是会 挂起 协程，并且只能在协程中使用。
                //用delay和sleep效果都一样
                //delay(2000)
                Thread.sleep(2000)
                Logger.i("Kotlin Coroutines")
            }

            Logger.i("1")

            /**
             * delay 是一个特殊的 挂起函数 ，它不会造成线程阻塞，但是会挂起协程，并且只能在协程中使用。
             *
             * 延迟 2 秒来保证 JVM 的存活,
             * 如果不加, 执行完2后jvm进程就结束了, 上面的Kotlin Coroutines不会打印
             */
            delay(2000)

            Logger.i("2")
        }
        //end会在runBlocking执行完后打印出来!
        Logger.i("end")
    }

    /**
     * Thread[main,5,main] -> 2
     * Thread[main,5,main] -> runBlocking delay
     * Thread[DefaultDispatcher-worker-2,5,main] -> 1
     * Thread[main,5,main] -> end
     */
    fun test2() {
        GlobalScope.launch {
            delay(1000)
            Logger.i(1)
        }

        Logger.i(2)

        /**
         * 调用了 runBlocking 的主线程会一直 阻塞 直到 runBlocking 内部的协程执行完毕
         *
         * runBlocking中的代码 是在当前线程中执行的, 所以delay是在main线程
         */
        runBlocking {
            Logger.i("runBlocking delay")
            delay(2000)
        }

        //end会在runBlocking执行完后打印出来!
        Logger.i("end")
    }
}