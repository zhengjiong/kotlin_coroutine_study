package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.zj.kotlin.coroutine.databinding.ActivityDemo66Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 *
 * CreateTime:2022/7/24 11:50
 * @author zhengjiong
 */
class Demo66Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDemo66Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_demo66)


        /**
         * buffer：指定固定容量的缓存；
         * 添加缓冲
         * 可以为buffer指定一个容量。不需要等待收集执行就立即执行发射数据，只是数据暂时被缓存而已，提高性能，如果我们只是单纯地添加缓存，而不是从根本上解决问题就始终会造成数据积压。
         */
        binding.button1.setOnClickListener {
            lifecycleScope.launch {
                flow {
                    for (i in 1..3) {
                        println("emit($i)  start")
                        delay(1000)
                        emit(i)
                        println("emit($i)  end")
                    }
                }.buffer().collect {
                    delay(1000)
                    println("collect --> $it")
                }
            }
        }

        /**
         * conflate：保留最新值；
         *
         * 当flow表示操作的部分结果或操作状态更新时，可能不需要处理每个值，而是只处理最近的值。
         */
        binding.button2.setOnClickListener {
            lifecycleScope.launch {
                flow {
                    for (i in 1..3) {
                        println("emit($i)  start")
                        delay(1000)
                        emit(i)
                        println("emit($i)  end")
                    }
                }.conflate().collect {
                    delay(1000)
                    println("collect --> $it")
                }
            }
        }

        /**
         * collectLatest：新值发送时，取消之前的。
         * 另一种方法是取消慢速收集器，并在每次发出新值时重新启动它。collectLatest在它们执行和 conflate操作符相同的基本逻辑，但是在新值上取消其块中的代码。
         */
        binding.button3.setOnClickListener {
            lifecycleScope.launch {
                flow {
                    for (i in 1..3) {
                        println("emit($i)  start")
                        delay(1000)
                        emit(i)
                        println("emit($i)  end")
                    }
                }.collectLatest {
                    delay(1000)
                    println("collect --> $it")
                }
            }
        }

        binding.button4.setOnClickListener {
            lifecycleScope.launch {
                whenStarted {
                    for (i in 0..100) {
                        delay(1000)
                        println("binding.button4--->$i")
                    }
                }
            }
        }

        binding.button5.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                for (i in 0..100) {
                    delay(1000)
                    println("binding.button5--->$i")
                }
            }
        }
    }

}