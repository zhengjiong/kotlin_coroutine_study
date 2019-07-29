package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by zhengjiong
 * date: 2019/7/29 21:56
 */

/**
 *
 * atomic和default很像,只是在cancel的时候不一样!
 *
 * cancel 调用一定会将该 job 的状态置为 cancelling，只不过ATOMIC 模式的协程在启动时无视了这一状态,
 * 在遇到第一个支持cancel的挂起点之后才会停止该协程, 所以2必定会被输出。
 *
 * 输出:
 * 22:02:54:745 [main] 1
 * 22:02:54:814 [DefaultDispatcher-worker-1] 2
 * 22:02:54:830 [DefaultDispatcher-worker-1] 4
 */
suspend fun main() {
    log(1)
    val job = GlobalScope.launch(start = CoroutineStart.ATOMIC) {
        log(2)
        delay(1000)
        log(3)
    }


    job.cancel()
    job.join()
    log(4)
}
