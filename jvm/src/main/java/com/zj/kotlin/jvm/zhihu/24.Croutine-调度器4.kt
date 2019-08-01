package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Created by zhengjiong
 * date: 2019/8/1 21:58
 */
suspend fun main() {
    log(1)

    /**
     * 为什么GlobalScope.launch指定了EmptyCoroutineContext也会发生调度切换线程,
     * 是因为通过launch启动的协程，如果你没有指定调度器，内部会给你默认一个默认调度器，所以会切换线程。
     */
    GlobalScope.launch(context = EmptyCoroutineContext) {
        log(2)
    }.join()

    log(3)
}