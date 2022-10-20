package com.zj.kotlin.coroutine

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zj.kotlin.coroutine.databinding.ActivityDemo77Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *
 * repeatOnLifecycle和launchWhenResumed的区别
 *
 * 1.launchWhenResumed只是挂起和恢复, 当生命周期小于Resumed的时候比如stoped(实际上不是小于,需要分析源
 * 码,Resumed对应的取消事件是ON_PAUSE, 当接收到ON_PAUSE的时候取消该协程)的时候挂起该线程,然后当回到Resumed
 * 的时候重新执行该协程,类似线程中的wait和notify
 *
 * 2.repeatOnLifecycle会在当前生命周期大于等于RESUMED的时候执行里面的方法,
 * 然后小于该生命周期后cancel掉该协程Job
 *
 * CreateTime:2022/7/24 11:50
 * @author zhengjiong
 */
class Demo78Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDemo77Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_demo77)
        binding.btnStart.setOnClickListener {
            startActivity(Intent(this, EmptyActivity::class.java))
        }
        binding.button1.setOnClickListener {

            //启动另一个activity或退到后台, 将会在能挂起的地方挂起(比如delay),然后在activity会到前台后继续从挂起点继续执行
            lifecycleScope.launchWhenStarted {
                var i = 1
                while (true) {
                    delay(3000)
                    println("button1  1  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                    i++
                }
            }

            //启动另一个activity或退到后台, 将会在能挂起的地方挂起(比如delay),然后在activity会到前台后继续从挂起点继续执行
            lifecycleScope.launchWhenResumed {
                var i = 1
                while (true) {
                    delay(3000)
                    println("button1  2  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                    i++
                }
            }

            //和上面两个不一样, 会在后台继续执行
            lifecycleScope.launchWhenCreated {
                var i = 1
                while (true) {
                    delay(3000)
                    println("button1  3  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                    i++
                }
            }
        }

        binding.button2.setOnClickListener {
            lifecycleScope.launch {
                //repeatOnLifecycle和launchWhenResumed的区别:
                /*1.launchWhenResumed只是挂起和恢复, 当生命周期小于Resumed的时候比如
                stoped(实际上不是小于,需要分析源码,Resumed对应的取消事件是ON_PAUSE, 当
                接收到ON_PAUSE的时候取消该协程)的时候挂起该线程,然后当回到Resumed的时候重
                新执行该协程,类似线程中的wait和notify

                2.repeatOnLifecycle会在当前生命周期大于等于RESUMED的时候执行里面的方法,
                然后小于该生命周期后cancel掉该协程Job*/
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    var i = 1
                    while (true) {
                        delay(3000)
                        println("button2  1  repeatOnLifecycle ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                        i++
                    }
                }
            }
        }
    }
}