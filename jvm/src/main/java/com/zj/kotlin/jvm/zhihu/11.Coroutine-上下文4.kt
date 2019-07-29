package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.suspendCoroutine

/**
 * Created by zhengjiong
 * date: 2019/7/29 23:06
 */

/**
 * 输出:
 * 23:35:46:108 [DefaultDispatcher-worker-1] StandaloneCoroutine{Active}@4d2445
 * 23:35:46:112 [DefaultDispatcher-worker-1] null
 */
suspend fun main() {
    /**
     * 为协程添加名称，方便调试
     * 如果有多个上下文需要添加，直接用 + 就可以
     */
    GlobalScope.launch(context = Dispatchers.Default + CoroutineName("zhengjiong")) {
        log(Job.Key.currentJob())
    }.join()

    log(Job.Key.currentJob())
}
