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
 * 把16.Coroutine-拦截器2的例子改为使用默认上下文, 2和3之间的delay也会切换线程
 *
 * 输出:
 *
 * 22:38:49:033 [main] start
 * 22:38:49:105 [DefaultDispatcher-worker-1] 1
 * 22:38:49:115 [DefaultDispatcher-worker-1] 4
 * 22:38:49:118 [DefaultDispatcher-worker-1] 2
 * 22:38:50:125 [DefaultDispatcher-worker-3] 3
 * 22:38:50:126 [DefaultDispatcher-worker-3] 5.aaaaaaaaaa
 * 22:38:50:126 [DefaultDispatcher-worker-3] end
 * 为什么从 3 处开始有了线程切换的操作？是因为我们的拦截器没有实现调度器的功能, 然后这个切换线程的逻辑源自于 delay，在 JVM 上 delay 实际上是在一个 ScheduledExcecutor
 * 里面添加了一个延时任务，因此会发生线程切换, 如果我们在拦截器当中自己处理了线程切换，那么就实现了自己的一个简单的调度器

 *
 */
suspend fun main() {
    log("start")
    GlobalScope.launch {
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