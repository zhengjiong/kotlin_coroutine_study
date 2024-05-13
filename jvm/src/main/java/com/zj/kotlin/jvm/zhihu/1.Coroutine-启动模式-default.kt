package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * https://zhuanlan.zhihu.com/p/61705548
 *
 *
 * Created by zhengjiong
 * date: 2019/7/29 21:10
 */

/**
 * suspend main 会启动一个协程, 它的上下文是空的(看RunSuspend)，因此 suspend main 启动的协程并不会有任何调度行为。
 *
 * 协程启动后会立即在当前线程执行, join是挂起点
 *
 * 输出:
 * 21:25:20:480 [main] 1
 * 21:25:20:552 [main] 3
 * 21:25:20:571 [DefaultDispatcher-worker-2] 2
 * 21:25:20:573 [DefaultDispatcher-worker-2] 4 或者 21:38:08:073 [main] 4
 */
suspend fun main() {
    log(1)
    val job = GlobalScope.launch(start = CoroutineStart.DEFAULT) {
        log(2)
    }

    log(3)
    //join后线程可能会切换, 也可能不会
    job.join()

    /**
     * 重要
     * 这里有两种结果:
     * 21:38:08:073 [main] 4
     * 21:25:20:573 [DefaultDispatcher-worker-2] 4
     * 原因是(结论):
     *  job.join 会等协程执行完毕，那么 join 挂起后面的代码会有两种情况：
     *  1. job 已经执行完了，那么立即执行后面的代码，此时在主线程
     *  2. job 还没有执行完，那么等待，job 执行完以后会在自己被调度的线程触发 resume，而外部 suspend main 其实是没有调度器的，因此直接在job 所在的调度器的线程执行，因此就是别的线程了
     *
     */
    log(4)
}
