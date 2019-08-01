package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Created by zhengjiong
 * date: 2019/7/31 23:08
 */

suspend fun main() {
    test1()
}

/**
 * test1这种写法其实等价于test2
 */
private suspend fun test1() {
    log(1)
    GlobalScope.launch {
        log(2)
    }.join()
    log(3)
}

private fun test2() {

    //结论:runSuspend位于RunSuspendKt文件中,RunSuspend其实是一个Continuation的实现,
    //它的上下文是空的，因此 suspend main 启动的协程并不会有任何调度行为。
    /*runSuspend {
        log(1)
        GlobalScope.launch {
            log(2)
        }.join()
        log(3)
    }*/

    /**
     * 上述代码在标准库当中被修饰为 internal，因此我们无法直接使用它们。不过你可以把 RunSuspend.kt 当中的内
     * 容复制到你的工程当中，这样你就可以直接使用啦，其中的 var result: Result<Unit>? = null 可能会报错，
     * 没关系，改成 private var result: Result<Unit>? = null 就可以了。
     */
}