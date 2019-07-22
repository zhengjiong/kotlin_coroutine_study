package com.zj.kotlin.jvm.bennyhuo2

import com.zj.kotlin.jvm.Logger
import kotlinx.coroutines.*

/**
 *
 * CreateTime:2019-07-19  09:50
 * @author 郑炯
 * @version 1.0
 */

fun main() {
    val example = Example3()
    example.test1()
}

class Example3 {

    val threadPoolDispatcher = newSingleThreadContext("myThreadPool")

    /**
     * 输出:
     * Thread[DefaultDispatcher-worker-1,5,main] -> 1
     * Thread[DefaultDispatcher-worker-1,5,main] -> 2
     * Thread[DefaultDispatcher-worker-1,5,main] -> 3
     * Thread[myThreadPool,5,main] -> -1
     * Thread[myThreadPool,5,main] -> -2
     */
    fun test1() {
        /**
         * 这里不加协程调度器:Dispatchers.Default, runBlocking就会运行在主线程
         */
        runBlocking(Dispatchers.Default) {
            Logger.i(1)

            /**
             * launch所启动的协程会运行在threadPoolDispatcher所创建的线程中
             */
            val job = launch(threadPoolDispatcher) {
                Logger.i(-1)
                delay(1000)
                Logger.i(-2)
            }

            Logger.i(2)
            Logger.i(3)
        }
    }
}