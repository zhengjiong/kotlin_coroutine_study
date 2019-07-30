package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 *
 * https://www.bennyhuo.com/2019/04/11/coroutine-dispatchers/
 *
 * 拦截器也是一个上下文的实现方向，拦截器可以左右你的协程的执行，同时为了保证它的功能的正确性，
 * 协程上下文集合永远将它放在最后面。它拦截协程的方法也很简单，因为协程的本质就是回调 + “黑魔法”，
 * 而这个回调就是被拦截的 Continuation 了。调度器就是基于拦截器实现的，
 * 换句话说调度器就是拦截器的一种。
 *
 * CreateTime:2019-07-30  17:28
 * @author 郑炯
 * @version 1.0
 */

/**
 * 首先，所有协程启动的时候，都会有一次 Continuation.resumeWith 的操作，
 * 这一次操作对于调度器来说就是一次调度的机会，我们的协程有机会调度到其他线程的关键之处就在于此.
 * 所以会有两次被拦截的打印
 *
 * 输出:
 * 17:52:44:766 [main] start
 * 17:52:44:900 [main] MyContinuationInterceptor interceptContinuation
 * 17:52:44:906 [main] MyContinuation resumeWith Success(kotlin.Unit)
 * 17:52:44:916 [main] 1
 * 17:52:44:920 [main] MyContinuationInterceptor interceptContinuation
 * 17:52:44:921 [main] MyContinuation resumeWith Success(kotlin.Unit)
 * 17:52:44:921 [main] 2
 * 17:52:44:922 [main] end
 */
suspend fun main() {
    log("start")
    GlobalScope.launch(context = MyContinuationInterceptor()) {
        log(1)
        launch {
            log(2)
        }
    }

    log("end")
}


class MyContinuationInterceptor : ContinuationInterceptor {
    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor.Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        log("MyContinuationInterceptor interceptContinuation")
        return MyContinuation(continuation)
    }

}

class MyContinuation<T>(val continuation: Continuation<T>) : Continuation<T> {
    override val context: CoroutineContext
        get() = continuation.context

    override fun resumeWith(result: Result<T>) {
        log("MyContinuation resumeWith $result")
        continuation.resumeWith(result)
    }

}