package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * https://zhuanlan.zhihu.com/p/61705548
 *
 * Created by zhengjiong
 * date: 2019/7/29 21:44
 */

/**
 * LAZY 是懒汉式启动，launch 后并不会有任何调度行为，协程体也自然不会进入执行状态，直到我们需要它执行的时候。
 *
 * 1.调用 Job.start，主动触发协程的调度执行
 * 2.调用 Job.join，隐式的触发协程的调度执行
 *
 * 输出:
 * 21:46:58:732 [main] 1
 * 21:46:58:785 [main] 3
 * 21:46:58:797 [DefaultDispatcher-worker-1] 2
 * 21:46:58:807 [main] 4 或者 21:47:51:602 [DefaultDispatcher-worker-1] 4
 */
suspend fun main() {
    log(1)
    val job = GlobalScope.launch(start = CoroutineStart.LAZY) {
        log(2)
    }

    log(3)
    //join后线程可能会切换, 也可能不会
    job.join()

    log(4)
}