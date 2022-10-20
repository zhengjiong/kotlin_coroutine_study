package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zj.kotlin.coroutine.databinding.ActivityDemo119Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 *
 *
0.StateFlow 默认replay为1

1.MutableSharedFlow 没有起始值

2.SharedFlow 可以保留历史数据

3.MutableSharedFlow 发射值需要调用 emit()/tryEmit() 方法，没有 setValue() 方法

4.与 MutableSharedFlow 不同，MutableSharedFlow 构造器中是不能传入默认值的，这意味着 MutableSharedFlow 没有默认值。

5.StateFlow 与 SharedFlow 还有一个区别是SateFlow只保留最新值，即新的订阅者只会获得最新的和之后的数据。
而 SharedFlow 根据配置可以保留历史数据，新的订阅者可以获取之前发射过的一系列数据。

6.StateFlow 的 emit() 和 tryEmit() 方法内部实现是一样的，都是调用 setValue()

7.StateFlow 默认是防抖的，在更新数据时，会判断当前值与新值是否相同，如果相同则不更新数据。

8.SharedFlow和StateFlow 执行tryEmit或者emit都不会等到collect收集后再继续执行(sharedFlow设置为
BufferOverflow.DROP_OLDEST不会挂起, 设置为BufferOverflow.suspend会挂起),而是直接继续执行后面的操作
 *
 *
 * CreateTime:2022/7/24 11:50
 * @author zhengjiong
 */
class Demo119Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDemo119Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_demo119)

        //replay设置为0, 如果emit的时候还没有执行到collect, replay设置为0的话, 那这一次emit就不会被collect到会丢失
        val mutableShareFlow1= MutableSharedFlow<String>(0, 2, BufferOverflow.DROP_OLDEST)
        val shareFlow1:SharedFlow<String> = mutableShareFlow1.asSharedFlow()

        //replay设置为0, 如果emit的时候还没有执行到collect, replay设置为0的话, 那这一次emit就不会被collect到会丢失
        binding.button1.setOnClickListener {
            lifecycleScope.launch{
                repeat(1000){
                    log("repeat  $it")
                    mutableShareFlow1.emit(it.toString())
                    //Shareflow没有value属性只能调用emit方法

                    delay(3000)
                }

            }

            //会丢失第一个,因为emit的时候这里还没collect
            lifecycleScope.launch {
                delay(200)
                mutableShareFlow1.collect{
                    log("1collect share -> $it")
                }
            }
        }

        binding.button2.setOnClickListener {
            lifecycleScope.launch {
                mutableShareFlow1.collect{
                    log("2collect share -> $it")
                }
            }
        }

//        System.out: 23:19:36:846 [Thread[main,5,main]]->button3 share -> 0
//        System.out: 23:19:36:848 [Thread[main,5,main]]->button3 emit  kotlin.Unit  --> 0
//        .....
//        System.out: 23:19:36:945 [Thread[main,5,main]]->button3 emit  kotlin.Unit  --> 997
//        System.out: 23:19:36:945 [Thread[main,5,main]]->button3 emit  kotlin.Unit  --> 998
//        System.out: 23:19:36:945 [Thread[main,5,main]]->button3 emit  kotlin.Unit  --> 999
//        System.out: 23:19:37:852 [Thread[main,5,main]]->button3 share -> 990
//        System.out: 23:19:38:857 [Thread[main,5,main]]->button3 share -> 991
//        System.out: 23:19:39:862 [Thread[main,5,main]]->button3 share -> 992
//        System.out: 23:19:40:868 [Thread[main,5,main]]->button3 share -> 993
//        System.out: 23:19:41:875 [Thread[main,5,main]]->button3 share -> 994
//        System.out: 23:19:42:879 [Thread[main,5,main]]->button3 share -> 995
//        System.out: 23:19:43:884 [Thread[main,5,main]]->button3 share -> 996
//        System.out: 23:19:44:891 [Thread[main,5,main]]->button3 share -> 997
//        System.out: 23:19:45:897 [Thread[main,5,main]]->button3 share -> 998
//        System.out: 23:19:46:901 [Thread[main,5,main]]->button3 share -> 999
        //会输出11次collect, 是因为extraBufferCapacity设置为10, 第一次接受到是因为delay方在最后
        //这里执行tryEmit或者emit都不会等到collect收集后再继续执行,而是直接继续执行后面的操作
        val mutableShareFlow3= MutableSharedFlow<String>(0, 10, BufferOverflow.DROP_OLDEST)
        binding.button3.setOnClickListener {
            lifecycleScope.launch{
                delay(3000)
                repeat(1000){
                    //这里执行tryemit或者emit都不会等到collect收集后再继续执行,而是直接继续执行后面的操作
                    val result = mutableShareFlow3.tryEmit(it.toString())
                    //Shareflow没有value属性只能调用emit方法

                    log("button3 emit  $result  --> $it")
                }

            }

            //会丢失第一个,因为emit的时候这里还没collect
            lifecycleScope.launch {
                mutableShareFlow3.collect{
                    log("button3 share -> $it")
                    delay(1000)
                }
            }
            mutableShareFlow3.onEach {
                log("onEach $it")
            }.launchIn(lifecycleScope)
        }

        val mutableShareFlow4= MutableSharedFlow<String>(0, 10, BufferOverflow.SUSPEND)
        binding.button4.setOnClickListener {
            lifecycleScope.launch{
                delay(3000)
                repeat(1000){
                    //这里执行tryemit或者emit都不会等到collect收集后再继续执行,而是直接继续执行后面的操作
                    val result = mutableShareFlow4.emit(it.toString())
                    //Shareflow没有value属性只能调用emit方法

                    log("button4 emit  $result  --> $it")
                }

            }

            //会丢失第一个,因为emit的时候这里还没collect
            lifecycleScope.launch {
                mutableShareFlow4.collect{
                    log("button4 share -> $it")
                    delay(1000)
                }
            }
        }

        val mutableStateFlow5= MutableStateFlow<String>("init")
        binding.button5.setOnClickListener {
            lifecycleScope.launch{
                delay(3000)
                repeat(1000){
                    //这里执行tryemit或者emit都不会等到collect收集后再继续执行,而是直接继续执行后面的操作
                    val result = mutableStateFlow5.tryEmit(it.toString())

                    log("button5 emit  $result  --> $it")
                }

            }

            //会丢失第一个,因为emit的时候这里还没collect
            lifecycleScope.launch {
                mutableStateFlow5.collect{
                    log("button5 share -> $it")
                    delay(1000)
                }
            }
        }


        val mutableStateFlow6 = MutableStateFlow("state init value")
        binding.button6.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Default) {
                repeat(1000){
                    log("repeat  $it")
                    //stateflow的emit方法最终也是调用的value的set方法(mutableStateFlow2.value = xxx)
                    mutableStateFlow6.emit(it.toString())
                    //mutableStateFlow2.value = it.toString()
                    //delay(3000)
                }

            }

            lifecycleScope.launch {
                delay(200)
                //这里只会输出999, 因为当collect的时候上面都emit完了, stateflow默认replay为1, 所有只会缓存一个
                mutableStateFlow6.collect{
                    log("collect state -> $it")
                }
            }
        }




        val mutableStateFlow9 = MutableStateFlow(0)
        val stateFlow9 = mutableStateFlow9.asStateFlow()
        fun increaseCountNum() {
            lifecycleScope.launch {
                while (true) {
                    delay(1000)
                    mutableStateFlow9.value++
                    //mutableStateFlow1.value++等价于mutableStateFlow1.emit(mutableStateFlow1.value+1)
                    //emit内部执行了:this.value = value
                    //mutableStateFlow1.emit(mutableStateFlow1.value+1)
                }
            }
        }
        binding.button9.setOnClickListener {
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
                    stateFlow9.collectLatest {
                        log("repeatOnLifecycle  collectLatest  $it")
                    }
                }
            }

        }
        binding.button10.setOnClickListener {
            increaseCountNum()
        }
    }
}