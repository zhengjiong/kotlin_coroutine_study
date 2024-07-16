package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.zj.kotlin.coroutine.databinding.ActivityDemo109Binding
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.concurrent.thread

/**
 * conflate 如果值的生产速度大于值的消耗速度，就忽略掉中间未来得及处理的值，只处理最新的值。
 * collectLatest和collect区别
 * 区别1：collectLatest当emit执行之后，collect会执行，但上游并没有挂起(collectLatest)，而是继续在emit之后执行
 * 区别2：当有新的值被emit，下游collectLatest没有被执行完会被cancel取消
 * 区别3：collectLatest不会挂起上下游线程,上游继续emit,下游一样可以收到,但是上游继续发送的话,下游如果被挂起就会被取消
 *
 * buffer conflate collectLatest 区别
 * 1.buffer会全部执行完不会取消某个collect
 * 2.collectLatest会取消collect块
 * 3.conflate不会影响collect执行，但是缓冲区有多个值的时候只会把最新的那个给collect
 *
 *
 * CreateTime:2022/7/24 11:50
 * @author zhengjiong
 */
class Demo109Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDemo109Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_demo109)

        val flow1 = flowOf(1, 2, 3)
        binding.button1.setOnClickListener {
            /**
             * 输出:
             * 16:00:07:590 [Thread[main,5,main]]->Result---3
             */
            lifecycleScope.launch {
                flow1.collectLatest {
                    delay(1000)
                    log("Result---$it")
                }
            }
        }

        val flow2 = flow<String> {
            repeat(5) {
                delay(400)
                emit(it.toString())
            }
        }.flowOn(Dispatchers.IO)
        /**
         * 输出:
         * 16:10:24:104 [Thread[main,5,main]]->collectLatest 0
         * 16:10:24:505 [Thread[main,5,main]]->collectLatest 1
         * 16:10:24:907 [Thread[main,5,main]]->collectLatest 2
         * 16:10:25:309 [Thread[main,5,main]]->collectLatest 3
         * 16:10:25:714 [Thread[main,5,main]]->collectLatest 4
         */
        binding.button2.setOnClickListener {
            lifecycleScope.launch {
                flow2.collectLatest {
                    log("collectLatest $it")
                }
            }
        }


        val flow3 = flow<String> {
            repeat(3) {
                emit(it.toString())
                delay(100)
            }
        }.flowOn(Dispatchers.IO)
        /**
         * 输出:
         * ->collectLatest start -> 0
         * ->collectLatest start -> 1
         * ->collectLatest start -> 2
         * ->collectLatest end -> 2
         *
         * 解释:
         * 会输出所有的原因是英文delay在下边, 所以delay会被取消多次
         *
         * 当第一个数据 "0" 发出时,立即开始收集并输出 collectLatest start -> 0
         * 在还未完成收集 "0" 的过程中,第二个数据 "1" 发出,此时 collectLatest 立即取消之前的收集,开始收集 "1" 并输出 collectLatest start -> 1
         * 同理,第三个数据 "2" 发出时,collectLatest 立即取消之前的收集,开始收集 "2" 并输出 collectLatest start -> 2
         * 最后,完成对 "2" 的收集,输出 collectLatest end -> 2
         */
        binding.button3.setOnClickListener {
            lifecycleScope.launch {
                flow3.collectLatest {
                    log("collectLatest start -> $it")
                    delay(1000)
                    log("collectLatest end -> $it")
                }
            }
        }

//        before send10
//        collect开始10
//        10
//        collect结束10
//        after send10
//
//        before send9
//        collect开始9
//        9
//        collect结束9
//        after send9
        binding.button4.setOnClickListener {
            lifecycleScope.launch {
                val flow = flow<Int> {
                    var currentValue = 10
                    log("before send$currentValue")
                    emit(currentValue)
                    log("after send$currentValue")
                    while (currentValue > 0) {
                        delay(5000)
                        currentValue--
                        log("before send$currentValue")
                        emit(currentValue)
                        log("after send$currentValue")
                    }
                }.collect {
                    log("collect开始$it")
                    delay(1500)
                    log(it)
                    log("collect结束$it")
                }
            }
        }

        //当emit执行之后，collect会执行，但上游并没有挂起，而是继续在emit之后执行，在这段代码中，
        // 因为collect中有delay函数，所以after send就先于 collect开始 打印了出来。
        binding.button5.setOnClickListener {
            lifecycleScope.launch {
                val flow = flow<Int> {
                    var currentValue = 10
                    log("before send$currentValue")
                    emit(currentValue)
                    log("after send$currentValue")
                    while (currentValue > 0) {
                        delay(5000)
                        currentValue--
                        log("before send$currentValue")
                        emit(currentValue)
                        log("after send$currentValue")
                    }
                }.collectLatest {
                    log("collect开始$it")
                    delay(2000)
                    log(it)
                    log("collect结束$it")
                }
            }
        }

        binding.button6.setOnClickListener {
            lifecycleScope.launch {
                val flow = flow<Int> {
                    var currentValue = 10
                    log("before send$currentValue")
                    emit(currentValue)
                    log("after send$currentValue")
                    while (currentValue > 0) {
                        delay(500)// 延迟500
                        currentValue--
                        log("before send$currentValue")
                        emit(currentValue)
                        log("after send$currentValue")
                    }
                }.collectLatest {// 使用collectLatest
                    log("collect开始$it")
                    delay(2000)// 延迟2000
                    log(it)
                    log("collect结束$it")
                }
            }
        }


        //客人吃完西瓜因为emit会挂起等collect执行完再resume，所以下一个菜要等客人吃完才上，那可不可以等客人一边吃就一边
        // 上菜呢？即要实现：collect不会令emit挂起，并保证emit的值按顺序到达，collect也对应的不取消(collectLatest就会取消)，
        // 也按顺序对应执行。
        //用buffer可以解决 看button8
        binding.button7.setOnClickListener {
            lifecycleScope.launch {
                // 模拟餐厅上菜
                flow<String> {
                    log("上菜——鸡肉")
                    emit("鸡肉")
                    delay(1000)
                    log("上菜——鱼肉")
                    emit("鱼肉")
                    delay(1000)
                    log("上菜——西瓜")
                    emit("西瓜")
                }.onEach {
                    log("运送$it")
                }.collect {
                    log("客人收到$it")
                    delay(2000)
                    log("客人吃完$it")
                }
            }
        }

        //buffer
        //客人吃完西瓜因为emit会挂起等collect执行完再resume，所以下一个菜要等客人吃完才上，那可不可以等客人一边吃就
        // 一边上菜呢？即要实现：collect不会令emit挂起，并保证emit的值按顺序到达，collect也对应的不取消(collectLatest就会取消)，也按顺序对应执行。
        //用buffer可以解决
//        System.out: 16:06:21:401 [Thread[main,5,main]]->上菜——A
//        System.out: 16:06:21:402 [Thread[main,5,main]]->运送A
//        System.out: 16:06:21:403 [Thread[main,5,main]]->客人收到A
//        System.out: 16:06:22:405 [Thread[main,5,main]]->上菜——B
//        System.out: 16:06:22:406 [Thread[main,5,main]]->运送B
//        System.out: 16:06:23:410 [Thread[main,5,main]]->上菜——C
//        System.out: 16:06:23:415 [Thread[main,5,main]]->运送C
//        System.out: 16:06:24:408 [Thread[main,5,main]]->客人吃完A
//        System.out: 16:06:24:413 [Thread[main,5,main]]->客人收到B
//        System.out: 16:06:27:422 [Thread[main,5,main]]->客人吃完B
//        System.out: 16:06:27:427 [Thread[main,5,main]]->客人收到C
//        System.out: 16:06:30:437 [Thread[main,5,main]]->客人吃完C
        binding.button8.setOnClickListener {
            lifecycleScope.launch {
                // 模拟餐厅上菜
                flow<String> {
                    log("上菜——A")
                    emit("A")
                    delay(1000)
                    log("上菜——B")
                    emit("B")
                    delay(1000)
                    log("上菜——C")
                    emit("C")
                }.onEach {
                    log("运送$it")
                }.buffer()// 增加buffer
                    .collect {
                        log("客人收到$it")
                        delay(5000)
                        log("客人吃完$it")
                    }
            }
        }

        //conflate
//        System.out: 16:09:54:450 [Thread[main,5,main]]->上菜——A
//        System.out: 16:09:54:452 [Thread[main,5,main]]->运送A
//        System.out: 16:09:54:453 [Thread[main,5,main]]->客人收到A
//        System.out: 16:09:55:455 [Thread[main,5,main]]->上菜——B
//        System.out: 16:09:55:456 [Thread[main,5,main]]->运送B
//        System.out: 16:09:56:461 [Thread[main,5,main]]->上菜——C
//        System.out: 16:09:56:466 [Thread[main,5,main]]->运送C
//        System.out: 16:09:57:458 [Thread[main,5,main]]->客人吃完A
//        System.out: 16:09:57:463 [Thread[main,5,main]]->客人收到C
//        System.out: 16:10:00:472 [Thread[main,5,main]]->客人吃完C
        //如果值的生产速度大于值的消耗速度，就忽略掉中间未来得及处理的值，只处理最新的值。
        //conflate不会影响collect执行，但是缓冲区有多个值的时候只会把最新的那个给collect。
        //A在delay的时候,后面又发送了B和C,这个时候collect正在delay中,conflate并不会取消A和B,会把最新的值给collect,
        //因为A在delay,所以A会输出,然后B不是最新所以跳过,最后输出C
        //这点和collectLatest不一样,collectLatest会取消A和B,值会输出C
        binding.button9.setOnClickListener {
            lifecycleScope.launch {
                // 模拟餐厅上菜
                flow<String> {
                    log("上菜——A")
                    emit("A")
                    delay(1000)
                    log("上菜——B")
                    emit("B")
                    delay(1000)
                    log("上菜——C")
                    emit("C")
                }.onEach {
                    log("运送$it")
                }.conflate()// 增加conflate
                    .collect {
                        log("客人收到$it")
                        delay(5000)
                        log("客人吃完$it")
                    }
            }
        }

//        System.out: 16:19:27:711 [Thread[main,5,main]]->上菜——A
//        System.out: 16:19:27:716 [Thread[main,5,main]]->运送A
//        System.out: 16:19:27:717 [Thread[main,5,main]]->客人收到A
//        System.out: 16:19:27:820 [Thread[main,5,main]]->上菜——B
//        System.out: 16:19:27:821 [Thread[main,5,main]]->运送B
//        System.out: 16:19:27:824 [Thread[main,5,main]]->客人收到B
//        System.out: 16:19:27:926 [Thread[main,5,main]]->上菜——C
//        System.out: 16:19:27:927 [Thread[main,5,main]]->运送C
//        System.out: 16:19:27:929 [Thread[main,5,main]]->客人收到C
//        System.out: 16:19:32:937 [Thread[main,5,main]]->客人吃完C

        //collectLatest和collect区别
        //* 区别1：当emit执行之后，collect会执行，但上游并没有挂起(collectLatest)，而是继续在emit之后执行
        //* 区别2：当有新的值被emit，下游collectLatest没有被执行完会被cancel取消(挂起处取消比如delay)
        //* 区别2补充：collectLatest不会挂起上下游线程,上游继续emit,下游一样可以收到,但是上游继续发送的话,下游如果被挂起就会被取消

//        buffer conflate collectLatest 区别
//        * 1.buffer会全部执行完不会取消某个collect
//        * 2.collectLatest会取消collect块
//        * 3.conflate不会影响collect执行，但是缓冲区有多个值的时候只会把最新的那个给collect

        //collectLatest
        //collectLatest会取消A和B,但是是在能被挂起的地方取消(delay处),所以会接收到"客人收到B"
        //最后在delay后只会输出"客人吃完C"
        binding.button10.setOnClickListener {
            lifecycleScope.launch {
                // 模拟餐厅上菜
                flow<String> {
                    log("上菜——A")
                    emit("A")
                    delay(1000)
                    log("上菜——B")
                    emit("B")
                    delay(1000)
                    log("上菜——C")
                    emit("C")
                }.onEach {
                    log("运送$it")
                }.collectLatest {
                    log("客人收到$it")
                    delay(1500)
                    log("客人吃完$it")
                }
            }
        }

//        System.out: flatMapLatest 2
//        System.out: flatMapLatest 3
//        System.out: flatMapLatest 4
        //flatMapLatest：与 collectLatest 操作符类似
        binding.button11.setOnClickListener {
            lifecycleScope.launch {
                flow {
                    emit("1")
                    emit("2")
                    delay(1000)
                    emit("3")
                    delay(1000)
                    emit("4")
                }.flatMapLatest { value ->
                    flow<String> {
                        delay(200)
                        println("flatMapLatest $value")
                    }
                }.collect()
            }
        }

//        I/System.out: collectLatest 2
//        I/System.out: collectLatest 3
//        I/System.out: collectLatest 4
        //flatMapLatest：与 collectLatest 操作符类似
        binding.button12.setOnClickListener {
            lifecycleScope.launch {
                flow {
                    emit("1")
                    emit("2")
                    delay(1000)
                    emit("3")
                    delay(1000)
                    emit("4")
                }.collectLatest {
                    delay(200)
                    println("collectLatest $it")
                }
            }
        }

        //4
        //flatMapLatest：与 collectLatest 操作符类似
        binding.button13.setOnClickListener {
            lifecycleScope.launch {
                flow {
                    emit("1")
                    delay(50)
                    emit("2")
                    delay(50)
                    emit("3")
                    delay(50)
                    emit("4")
                }.collectLatest {
                    delay(500)
                    println("collectLatest $it")
                }
            }
        }

        //4
        //flatMapLatest：与 collectLatest 操作符类似
        binding.button14.setOnClickListener {
            lifecycleScope.launch {
                flow {
                    emit("1")
                    delay(50)
                    emit("2")
                    delay(50)
                    emit("3")
                    delay(50)
                    emit("4")
                }.flatMapLatest {
                    flow<String> {
                        delay(500)
                        println("collectLatest $it")
                    }
                }.collect()
            }
        }
    }
}