package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 *
 * CreateTime:2019-07-31  10:44
 * @author 郑炯
 * @version 1.0
 */


/**
 * 由于这个线程池是我们自己创建的，因此我们需要在合适的时候关闭它,
 * 不然进程就不会停止.
 *
 * 废弃的两个基于线程池创建调度器的 API:
 * 1.fun newSingleThreadContext(name: String): ExecutorCoroutineDispatcher
 * 2.fun newFixedThreadPoolContext(nThreads: Int, name: String): ExecutorCoroutineDispatcher
 * 这二者可以很方便的创建绑定到特定线程的调度器，但过于简洁的 API 似乎会让人忘记它的风险。Kotlin 一向不爱做
 * 这种不清不楚的事儿，所以你呢，还是像我们这一节例子当中那样自己去构造线程池吧，这样好歹自己忘了关闭也怨不着别人。
 *
 *
 *
 * 输出:
 * 22:06:28:960 [main] start
 * 22:06:29:058 [main] newThread
 * 22:06:29:059 [MyThread] 1
 * 22:06:29:068 [main] end 或者 [MyThread] end

 */
suspend fun main() {
    log("start")
    val myDispatcher = Executors.newSingleThreadExecutor(object : ThreadFactory {
        override fun newThread(r: Runnable?): Thread {
            log("newThread")
            return Thread(r, "MyThread")
        }
    }).asCoroutineDispatcher()

    GlobalScope.launch(context = myDispatcher) {
        log(1)
    }.join()
    //这里必须手动关闭, 不然进程不会停止
    myDispatcher.close()
    log("end")

    //废弃的两个基于线程池创建调度器的API:
    //newSingleThreadContext("aaa").close()
    //newFixedThreadPoolContext(1, "bbb").close()
}