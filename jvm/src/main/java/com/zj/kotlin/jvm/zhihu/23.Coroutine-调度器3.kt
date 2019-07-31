package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.*

/**
 * delay:
 * delay，在 JVM 上 delay 实际上是在一个 ScheduledExcecutor 里面添加了一个延时任务，
 * 因此会发生线程切换；而在 JavaScript 环境中则是基于 setTimeout，如果运行在 Nodejs 上，
 * delay 就不会切线程了，毕竟人家是单线程的。
 *
 * CreateTime:2019-07-31  10:58
 * @author 郑炯
 * @version 1.0
 */


/**
 * 这里面除了 delay 那里有一次不可避免的线程切换外，其他几处协程挂起点的继续操作（Continuation.resume）都会切线程.
 *
 *
 *
 *
 */
suspend fun main() {
    log("start")
    /*Executors.newFixedThreadPool(5)
        .asCoroutineDispatcher().use { dispatcher ->*/
            GlobalScope.launch {
                log(1)

                //async和GlobalScope.launch他俩是同一个调度器，而且是默认调度器，默认调度器调度的时候是有可能调度到一个线程的.
                val job = async {
                    log(2)
                    delay(2000)
                    log(3)
                    "result"
                }
                log(4)
                val result = job.await()
                log("5 result=$result")
            }.join()
            log("end")
        /*}*/
}