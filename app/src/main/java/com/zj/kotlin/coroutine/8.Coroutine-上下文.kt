package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_demo8.*
import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine

/**
 *
 * CreateTime:2020/8/17 13:29
 * @author zhengjiong
 */
class Demo8Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo8)

        btn1.setOnClickListener {
            try {
                lifecycleScope.launch(CoroutineName("线程-1") + Dispatchers.Main) {
                    //获取当前协程的job
                    val job = coroutineContext[Job.Key]
                    println(job?.isActive)
                    //获取coroutineName
                    println(coroutineContext[CoroutineName.Key])
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}