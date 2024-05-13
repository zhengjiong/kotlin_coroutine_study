package com.zj.kotlin.coroutine

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.zj.kotlin.coroutine.databinding.ActivityDemo150Binding
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
class Demo150Activity : AppCompatActivity(R.layout.activity_demo150) {
    val viewModel by viewModels<Demo150ViewModel>()


    @OptIn(FlowPreview::class)
    override fun setContentView(layoutResID: Int) {
        val inflateView = layoutInflater.inflate(layoutResID, null)
        setContentView(inflateView)
        val binding = DataBindingUtil.bind<ActivityDemo150Binding>(inflateView)!!

        println("setContentView activity=${this.hashCode()}  viewModel=${viewModel}")


        binding.button1.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {

            }
        }


        val textView = findViewById<TextView>(R.id.text_view)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stateFlow.collect { time ->
                    textView.text = time.toString()
                }
            }
        }
    }

    /**
     * 手机横竖屏切换会导致Activity重新创建， 重新创建就会使得timeFlow重新被collect，
     * 而冷流每次被collect都是要重新执行的。
     */
    class Demo150ViewModel : ViewModel() {
        private val timeFlow = flow {
            var time = 0
            while (true) {
                emit(time)
                delay(1000)
                time++
            }
        }

        //stateIn函数接收3个参数，其中第1个参数是作用域，
        // 传入viewModelScope即可。
        // 第3个参数是初始值，计时器的初始值传入0即可。
        /**
        第2个参数则是最有意思的了。刚才有说过，当手机横竖屏切换的时候，我们不希望Flow停止工作。但是再之前又提到了，当程序切到后台时，我们希望Flow停止工作。
        这该怎么区分分别是哪种场景呢？
        Google给出的方案是使用超时机制来区分。
        因为横竖屏切换通常很快就能完成，这里我们通过stateIn函数的第2个参数指定了一个5秒的超时时长，那么只要在5秒钟内横竖屏切换完成了，Flow就不会停止工作。
        反过来讲，这也使得程序切到后台之后，如果5秒钟之内再回到前台，那么Flow也不会停止工作。但是如果切到后台超过了5秒钟，Flow就会全部停止了。
         */
        val stateFlow =
            timeFlow.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )
    }
}