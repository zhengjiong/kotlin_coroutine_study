package com.zj.kotlin.jvm.shengshiyuan

import com.zj.kotlin.jvm.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 *
 * CreateTime:2019-07-17  17:49
 * @author 郑炯
 * @version 1.0
 */

fun main() {
    val demo = HelloCoroutine3()
    //demo.test1()
    demo.test2()
}

class HelloCoroutine3 {
    /**
     * Thread[DefaultDispatcher-worker-1,5,main] -> delay
     * Thread[main,5,main] -> 2
     * Thread[main,5,main] -> join
     * Thread[DefaultDispatcher-worker-1,5,main] -> 1
     * Thread[main,5,main] -> end
     */
    fun test1() {
        runBlocking {
            val job = GlobalScope.launch {
                //这里打印delay可能在2的前面也可能在之后
                Logger.i("delay")
                delay(1000)
                Logger.i("1")
            }
            Logger.i(2)

            /**
             * join是挂起当前的协成知道job执行结束才继续执行后面的代码.
             *
             * 等待job协程执行结束,再继续执行后面的代码, 就可以不用delay了, 这样更优雅些
             */
            Logger.i("join")
            job.join()

            Logger.i("end")
        }
    }

    /**
     * Thread[DefaultDispatcher-worker-1,5,main] -> 0
     * Thread[main,5,main] -> end
     */
    fun test2() {
        runBlocking {
            GlobalScope.launch {
                Logger.i(0)
                delay(1000)
                Logger.i(1)

                GlobalScope.launch {
                    Logger.i(2)
                    delay(2000)
                    Logger.i(3)
                }.join()
            }.join()

            Logger.i("runBlocking end")
        }
        Logger.i("end")
    }
}