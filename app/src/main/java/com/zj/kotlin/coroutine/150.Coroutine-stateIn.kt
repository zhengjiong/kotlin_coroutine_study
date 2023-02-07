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

    class Demo150ViewModel : ViewModel() {
        private val timeFlow = flow {
            var time = 0
            while (true) {
                emit(time)
                delay(1000)
                time++
            }
        }

        val stateFlow =
            timeFlow.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )
    }
}