package com.zj.kotlin.coroutine

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.zj.kotlin.coroutine.databinding.ActivityDemo151Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 *
 * CreateTime:2023/1/11 16:41
 * @author zhengjiong
 */
class Demo151Activity : AppCompatActivity(R.layout.activity_demo151) {

    @OptIn(FlowPreview::class)
    override fun setContentView(layoutResID: Int) {
        val inflateView = layoutInflater.inflate(layoutResID, null)
        setContentView(inflateView)
        val binding = DataBindingUtil.bind<ActivityDemo151Binding>(inflateView)!!

        println("setContentView activity=${this.hashCode()}")

        //SharingStarted.Eagerly例子
        binding.button1.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                // 模拟一个快速发射的数据流
                val coldFlow = flow {
                    println("开始发射数据")
                    delay(10)    // 短暂延迟
                    emit("配置A")  // 立即发射
                    //delay(200)    // 短暂延迟
                    emit("配置B")
                    //delay(1000)
                    emit("配置C")
                    for (i in 0 .. 100) {
                        delay(1000)
                        //只会收到一次, 因为stateflow
                        emit("配置C")
                    }

                }

                val startTime = System.currentTimeMillis()

                val hotFlow = coldFlow.stateIn(
                    scope = this,
                    started = SharingStarted.Eagerly,
                    initialValue = "默认配置"
                )

                println("StateFlow创建完成，时间差: ${System.currentTimeMillis() - startTime}ms")

                // 立即订阅
                val job1 = launch {
                    hotFlow.collect { value ->
                        println("收集者1在${System.currentTimeMillis() - startTime}ms收到: $value")
                    }
                }

                delay(550)  // 非常短暂的延迟

                // 第二个订阅者
                val job2 = launch {
                    hotFlow.collect { value ->
                        println("收集者2在${System.currentTimeMillis() - startTime}ms收到: $value")
                    }
                }

                delay(1200)
                job1.cancel()
                job2.cancel()
            }
        }

    }
}