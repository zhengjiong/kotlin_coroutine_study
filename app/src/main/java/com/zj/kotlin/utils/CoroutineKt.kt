package com.zj.kotlin.utils

import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext

/**
 *
 * CreateTime:2020/8/17 11:32
 * @author zhengjiong
 */
//仿照 Thread.currentThread() 来一个获取当前 Job 的方法：
suspend fun Job.Key.currentJob() = coroutineContext[Job.Key]