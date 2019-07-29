package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by zhengjiong
 * date: 2019/7/29 22:04
 */

/**
 * 程在这种模式下会直接开始在当前线程下执行，直到第一个挂起点，这听起来有点儿像前面的 ATOMIC，
 * 不同之处在于 UNDISPATCHED 不经过任何调度器即开始执行协程体。当然遇到挂起点之后的执行就取
 * 决于挂起点本身的逻辑以及上下文当中的调度器了。
 */

/**
 * 输出:
 * 22:06:42:957 [main] 1
 * 22:06:43:011 [main] 2
 * 22:06:43:029 [main] 4
 * 22:06:44:031 [DefaultDispatcher-worker-1] 3
 * 22:06:44:035 [DefaultDispatcher-worker-1] 5
 */
suspend fun main() {
    log(1)
    val job = GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
        //协程启动后会立即在当前线程执行
        log(2)
        //delay 是挂起点
        delay(1000)
        /**
         * 3和5为什么在其他线程执行:
         * 我们的示例都运行在 suspend main 函数当中，所以 suspend main 函数
         * 会帮我们直接启动一个协程，而我们示例的协程都是它的子协程，所以这里 5 的调度取决于这个
         * 最外层的协程的调度规则了。关于协程的调度，我们后面研究。
         */
        log(3)
    }
    log(4)
    job.join()
    log(5)
}