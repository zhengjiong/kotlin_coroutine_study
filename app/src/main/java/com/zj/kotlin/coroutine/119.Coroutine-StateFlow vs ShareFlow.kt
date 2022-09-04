package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zj.kotlin.coroutine.databinding.ActivityDemo119Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 *
 * CreateTime:2022/7/24 11:50
 * @author zhengjiong
 */
class Demo119Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDemo119Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_demo119)

        val mutableStateFlow1 = MutableStateFlow(0)
        val stateFlow1 = mutableStateFlow1.asStateFlow()

        fun increaseCountNum() {
            lifecycleScope.launch {
                while (true) {
                    delay(1000)
                    mutableStateFlow1.value++
                }
            }
        }


        /*lifecycleScope.launch {
            stateFlow1.collectLatest {
                log("collectLatest   $it")
            }
        }*/


//        这是StateFlow的一个使用实例，一开始不明白为什么需要repeatOnLifecycle，运行了实例之后才醒悟，
//        collectLatest是一个suspend function，永远suspend(如果有代码在collectLatest块后面，会永远不会执行)，
//        那么lifecycleScope的销毁是在destroy之后，也就是说除非activity destroy，不然就一直collect，相当于
//        livedata一直观察着数据，即使activity不可见。

//        运行代码，发现跳转到另外一个activity(原activity处于stop状态)，依然打印着日志。为了达到与livedata同样
//        的生命周期效果，需要采用注释的那段代码。repeatOnLifecycle(Lifecycle.State.STARTED{}里的协程作用域会
//        检测到处于start状态就启动，检测到stop状态就取消。
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                println("repeatOnLifecycle   invoke")
                stateFlow1.collectLatest {
                    log("repeatOnLifecycle  collectLatest  $it")
                }
            }
        }
        binding.button1.setOnClickListener {
            increaseCountNum()
        }

    }
}