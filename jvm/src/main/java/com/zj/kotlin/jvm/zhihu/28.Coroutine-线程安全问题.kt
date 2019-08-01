package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 *
 * 结论:
 * 线程安全问题
 * Js 和 Native 的并发模型与 Jvm 不同，Jvm 暴露了线程 API 给用户，这也使得协程的调度可以由用户更灵活的选择。
 * 越多的自由，意味着越多的代价，我们在 Jvm 上面编写协程代码时需要明白一点的是，线程安全问题在调度器不同的协程之间仍然存在。
 * 好的做法就是尽量把自己的逻辑控制在一个线程之内，这样一方面节省了线程切换的开销,另一方面还可以避免线程安全问题，
 * 如果大家在协程代码中使用锁之类的并发工具就反而增加了代码的复杂度，对此我的建议是大家在编写协程代码时尽量避免对
 * 外部作用域的可变变量进行引用，尽量使用参数传递而非对全局变量进行引用。
 * 另一方面还可以避免线程安全问题，两全其美。
 *
 * Created by zhengjiong
 * date: 2019/7/31 22:39
 */


suspend fun main() {
    //test1()
    test2()
}


/**
 * 错误的写法
 * 输出:
 * [main] start
 * [main] start 0
 * [main] start 1
 * [main] start 2
 *
 * [pool-1-thread-1] launch 0
 * [pool-1-thread-2] launch 1
 * [pool-1-thread-3] launch 2
 * [pool-1-thread-4] launch 3
 *
 * [main] forEach join
 * [main] forEach join
 * [main] forEach join
 * .....
 * [main] 999786
 * [main] end
 */
private suspend fun test1() {
    var i = 0
    log("start")//main线程
    Executors.newFixedThreadPool(10)
        .asCoroutineDispatcher()
        .use { dispatcher: ExecutorCoroutineDispatcher ->
            List(1000000) { index ->
                log("start $index")//main线程
                GlobalScope.launch(dispatcher) {
                    i++
                    log("launch $index")
                }

            }.forEach {
                it.join()

                //forEach join会在end之前,然后launch $index之后输出, 因为前面一直join等待了.
                log("forEach join")
            }
            log(i)

        }
    log("end")
}

/**
 * 这种写法能得到正确的结果, 因为list在初始化的时候每次循环都需要等待执行协程体中的代码执行完成, 才开始下一次循环.
 * 输出:
 * 2019-07-31 23:05:23: [main] start
 * 2019-07-31 23:05:23: [main] 1
 * 2019-07-31 23:05:27: [pool-1-thread-3] 1000000
 * 2019-07-31 23:05:27: [pool-1-thread-3] end
 *
 */
private suspend fun test2() {
    var i = 0
    log("start")
    Executors.newFixedThreadPool(10)
        .asCoroutineDispatcher()
        .use { dispatcher: ExecutorCoroutineDispatcher ->
            log(1)
            List(1000000) { index ->
                //log("start $index")
                GlobalScope.launch(dispatcher) {
                    i++
                    //log("launch $index")
                }.join()
                //log("end $index")
            }
            log(i)

        }
    log("end")
}