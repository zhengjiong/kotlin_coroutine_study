package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.zj.kotlin.coroutine.databinding.ActivityDemo130Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 *
 * CreateTime:2023/1/11 16:41
 * @author zhengjiong
 */
class Demo130Activity : AppCompatActivity(R.layout.activity_demo130) {

    @OptIn(FlowPreview::class)
    override fun setContentView(layoutResID: Int) {
        //super.setContentView(layoutResID)
        val inflateView = layoutInflater.inflate(layoutResID, null)
        setContentView(inflateView)
        val binding = DataBindingUtil.bind<ActivityDemo130Binding>(inflateView)!!

        //sample是采样的意思，也就是说，它可以从flow的数据流当中按照一定的时间间隔来采样某一条数据。
        //这个函数在某些源数据量很大，但我们又只需展示少量数据的时候比较有用。
        //sample每隔1000采样一次
        binding.button1.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                //这里我们在flow构建函数中写了一个死循环，不断地在发送数据，那么这个数据的发送量无疑是巨大的。
                //而接下来我们借助sample函数进行数据采集，每秒钟只取一条数据。
                flow<Long> {
                    while (true) {
                        println("emit ${Thread.currentThread()}")
                        emit(System.currentTimeMillis())
                        delay(50)
                    }
                }.sample(1000).collect {
                    println("collect $it  ${Thread.currentThread()}")
                }
            }
        }
    }
}