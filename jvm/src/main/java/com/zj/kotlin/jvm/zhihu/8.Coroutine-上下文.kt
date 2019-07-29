package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * https://zhuanlan.zhihu.com/p/64330051
 * https://www.bennyhuo.com/2019/04/08/coroutines-start-mode/
 *
 * CoroutineContext 作为一个集合，它的元素就是源码中看到的 Element，每一个 Element 都有一个 key，
 * 因此它可以作为元素出现，同时它也是 CoroutineContext 的子接口，因此也可以作为集合出现。
 *
 * Created by zhengjiong
 * date: 2019/7/29 22:20
 */

/**
 * 我们在协程体里面访问到的 coroutineContext 大多是这个 CombinedContext 类型，
 * 表示有很多具体的上下文实现的集合，我们如果想要找到某一个特别的上下文实现，
 * 就需要用对应的 Key 来查找
 *
 * 输出:
 * 22:42:53:018 [DefaultDispatcher-worker-2] 2-context->StandaloneCoroutine{Active}@7c500e
 * 22:42:53:019 [DefaultDispatcher-worker-2] 2-job->kotlinx.coroutines.Job$Key@181416e
 * 22:42:53:021 [DefaultDispatcher-worker-3] 3-context->StandaloneCoroutine{Active}@dffd2c
 * 22:42:53:021 [DefaultDispatcher-worker-3] 3-job->kotlinx.coroutines.Job$Key@181416e
 * 22:42:53:024 [DefaultDispatcher-worker-2] 4-context->StandaloneCoroutine{Active}@1f23bd
 * 22:42:53:025 [DefaultDispatcher-worker-2] 4-job->kotlinx.coroutines.Job$Key@181416e
 * null
 */
suspend fun main() {
    GlobalScope.launch {
        //用当前job的key来获取Element, Element其实也是一个CoroutineScope上下文
        //[DefaultDispatcher-worker-2] 2-context->StandaloneCoroutine{Active}@7c500e
        log("2-context->"+coroutineContext[Job.Key])

        // [DefaultDispatcher-worker-2] 2-job->kotlinx.coroutines.Job$Key@181416e
        log("2-job->"+Job.Key)

        GlobalScope.launch {
            //[DefaultDispatcher-worker-3] 3-context->StandaloneCoroutine{Active}@dffd2c
            log("3-context->"+coroutineContext[Job.Key])

            // [DefaultDispatcher-worker-3] 3-job->kotlinx.coroutines.Job$Key@181416e
            log("3-job->"+Job.Key)
        }.join()

        launch {
            //[DefaultDispatcher-worker-2] 4-context->StandaloneCoroutine{Active}@1f23bd
            log("4-context->"+coroutineContext[Job.Key])

            // [DefaultDispatcher-worker-1] 4-job->kotlinx.coroutines.Job$Key@181416e
            log("4-job->"+Job.Key)
        }

        //这里获取到的Element(上下文)和"2-context->这里获取到的是一样的
        log("5-context->"+coroutineContext[Job.Key])

    }.join()

    /**
     * suspend main 虽然也是协程体，但它是更底层的逻辑，因此没有 Job 实例
     * 输出: null
     */
    println(coroutineContext[Job.Key])
}