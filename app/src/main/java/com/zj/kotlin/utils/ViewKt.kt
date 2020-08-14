package com.zj.kotlin.utils

import android.view.View
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 *
 * CreateTime:2020/8/14 17:03
 * @author zhengjiong
 */

suspend fun View.awaitNextLayout() = suspendCancellableCoroutine<Unit> { cont ->
    // 这里的 lambda 表达式会被立即调用，允许我们创建一个监听器
    val listener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View?,
            left: Int, top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            // 视图的下一次布局任务被调用
            // 先移除监听，防止协程泄漏
            removeOnLayoutChangeListener(this)
            // 最终，唤醒协程，恢复执行
            cont.resume(Unit)
        }
    }
    // 如果协程被取消，移除该监听
    cont.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
    // 最终，将监听添加到 view 上
    addOnLayoutChangeListener(listener)

    // 这样协程就被挂起了，除非监听器中的 cont.resume() 方法被调用
}