package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.*
import java.util.concurrent.Executors

/**
 * delay:
 * delay，在 JVM 上 delay 实际上是在一个 ScheduledExcecutor 里面添加了一个延时任务，
 * 因此会发生线程切换；而在 JavaScript 环境中则是基于 setTimeout，如果运行在 Nodejs 上，
 * delay 就不会切线程了，毕竟人家是单线程的。
 *
 *
 * CreateTime:2019-07-31  10:58
 * @author 郑炯
 * @version 1.0
 */


/**
 * 对比这二者，10个线程的情况线程切换次数最少 3次，而 1 个线程的情况则只要 delay 1000ms 之后恢复执行的时候那一次。
 *
 * 结论:除了 delay 那里有一次不可避免的线程切换外，其他几处协程挂起点的继续操作（Continuation.resume）都会切线程.
 * launch的时候会发生Continuation.resume，所以才会有协程启动时的第一次调度
 *
 * 下面的代码其实是很消耗性能的写法, 不应该创建这么多线程这么多线程应该直接用Executors.newSingleThreadExecutor(),
 * 就不会造成频繁的线程切换.
 *
 * 2019-07-31 22:20:18: [DefaultDispatcher-worker-1] 1
 * 2019-07-31 22:20:18: [DefaultDispatcher-worker-1] 4
 * 2019-07-31 22:20:18: [DefaultDispatcher-worker-3] 2
 * 2019-07-31 22:20:20: [DefaultDispatcher-worker-2] 3
 * 2019-07-31 22:20:20: [DefaultDispatcher-worker-2] 5 result=result
 * 2019-07-31 22:20:20: [DefaultDispatcher-worker-2] end
 *
 *
 *
 */
suspend fun main() {
    log("start")
    //这里应该使用Executors.newSingleThreadExecutor()才能保证性能.
    Executors.newFixedThreadPool(10)
        .asCoroutineDispatcher().use { dispatcher ->
            GlobalScope.launch {
                log(1)

                //结论:async和GlobalScope.launch是同一个调度器，而且是默认调度器，默认调度器调度的时候是有可能调度到同一个线程或者不同的线程.
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
        }
}