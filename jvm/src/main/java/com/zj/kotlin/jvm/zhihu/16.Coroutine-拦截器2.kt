package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *
 * CreateTime:2019-07-30  17:55
 * @author 郑炯
 * @version 1.0
 */

/**
 * 输出:
 *
 *
 * 22:23:13:954  [main] start
 * 222:23:14:004 [main] MyContinuationInterceptor interceptContinuation
 * 222:23:14:007 [main] MyContinuation resumeWith Success(kotlin.Unit)
 * 222:23:14:007 [main] 1
 * 222:23:14:011 [main] MyContinuationInterceptor interceptContinuation
 * 222:23:14:011 [main] MyContinuation resumeWith Success(kotlin.Unit)
 * 222:23:14:011 [main] 2
 * 222:23:14:022 [main] 4
 * 222:23:14:023 [main] MyContinuationInterceptor interceptContinuation
 * 222:23:15:020 [kotlinx.coroutines.DefaultExecutor] MyContinuation resumeWith Success(kotlin.Unit)
 * 222:23:15:020 [kotlinx.coroutines.DefaultExecutor] 3
 * 222:23:15:022 [kotlinx.coroutines.DefaultExecutor] MyContinuation resumeWith Success(aaaaaaaaaa)
 * 222:23:15:022 [kotlinx.coroutines.DefaultExecutor] 5.aaaaaaaaaa
 * 222:23:15:022 [kotlinx.coroutines.DefaultExecutor] end
 *
 *
 *
 *
 * 通过 launch 启动了一个协程，为它指定了我们自己的拦截器作为上下文，紧接着在其中用 async 启动了一个协程，async 与 launch 从
 * 功能上是同等类型的函数，它们都被称作协程的 Builder 函数，不同之处在于 async 启动的 Job 也就是实际上的 Deferred 可以有返回
 * 结果，可以通过 await 方法获取。
 *
 * 所有协程启动的时候，都会有一次 Continuation.resumeWith 的操作，这一次操作对于调度器来说就是一次调度的机会，
 * 我们的协程有机会调度到其他线程的关键之处就在于此。
 *
 *
 * 为什么从 3 处开始有了线程切换的操作？是因为我们的拦截器没有实现调度器的功能, 然后这个切换线程的逻辑源自于 delay，在 JVM 上 delay 实际上是在一个 ScheduledExcecutor
 * 里面添加了一个延时任务，因此会发生线程切换, 如果我们在拦截器当中自己处理了线程切换，那么就实现了自己的一个简单的调度器

 *
 */
suspend fun main() {
    log("start")
    GlobalScope.launch(MyContinuationInterceptor()) {
        log(1)
        val job = async {
            log(2)
            delay(1000)
            log(3)
            "aaaaaaaaaa"
        }
        log(4)
        val result = job.await()
        log("5.$result")

    }.join()
    log("end")
}