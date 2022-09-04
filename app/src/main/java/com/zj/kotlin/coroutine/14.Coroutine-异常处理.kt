package com.zj.kotlin.coroutine

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

/**
 *
 * SupervisorJob 可以用来改变我们的协程异常传递方式，从而让子协程自行处理异常。但需要注意的是，因为协程具有结构化
 * 的特点，SupervisorJob 仅只能用于同一级别的子协程。如果我们在初始化 scope 时添加了 SupervisorJob ,那么整个
 * scope对应的所有 根协程 都将默认携带 SupervisorJob ，否则就必须在 CoroutineContext 显示携带 SupervisorJob。
 *
 * CreateTime:2022/4/30 20:00
 * @author zhengjiong
 */
class Demo14Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo14)

        //不会闪退
        findViewById<Button>(R.id.button1).setOnClickListener {
            val scope = CoroutineScope(CoroutineExceptionHandler { coroutineContext, throwable ->
                //这里会收到协程A抛出的异常
                println("throwable ->  ${throwable.message}")
            })
            scope.launch(CoroutineName("A")) {
                delay(100)
                //这里抛出异常自己又不处理的话,会传递给父协程,从而造成下方的协程B也被取消
                throw RuntimeException("协程A抛出的异常")
            }
            scope.launch(CoroutineName("B")) {
                delay(1000)
                println("不能正常执行,会受影响")
            }
        }


        //不会闪退
        findViewById<Button>(R.id.button2).setOnClickListener {
            //supervisorJob 是一个特殊的Job,其会改变异常的传递方式，当使用它时,我们子协程的失败不会影响到其他子
            // 协程与父协程，通俗点理解就是:子协程会自己处理异常，并不会影响其兄弟协程或者父协程
            val scope =
                CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { coroutineContext, throwable ->
                    //这里会收到协程A抛出的异常
                    println("throwable ->  ${throwable.message}")
                })
            scope.launch(CoroutineName("A")) {
                delay(100)
                throw RuntimeException("协程A抛出的异常")
            }
            scope.launch(CoroutineName("B")) {
                delay(1000)
                //当协程A失败时，协程B依然可以正常打印。,下方会正常这行
                println("正常执行,我不会收到影响")
            }
        }

        //不会闪退
        findViewById<Button>(R.id.button3).setOnClickListener {
            val scope = CoroutineScope(CoroutineExceptionHandler { coroutineContext, throwable ->
                //这里会收到协程A抛出的异常
                println("throwable ->  ${throwable.message}")
            })
            scope.launch {
                //这里单独使用SupervisorJob,那它所抛出的异常不会让B协程受影响
                launch(CoroutineName("A") + SupervisorJob()) {
                    delay(100)
                    throw RuntimeException("协程A抛出的异常")
                }
                launch(CoroutineName("B")) {
                    delay(1000)
                    println("正常执行,我不会收到影响")
                }
            }
        }

        //闪退
        findViewById<Button>(R.id.button4).setOnClickListener {
            val scope = CoroutineScope(Job())
            //这里try不到的, 就算把Job()换成SupervisorJob()也是不行的
            // 我们在 launch 时，因为启动了一个新的协程作用域，而新的作用域内部已经是新的线程(可以理解为)，
            // 因为内部发生异常时因为没有被直接捕获 , 再加上其Job不是 SupervisorJob ,所以异常将向上开始传递,
            // 因为其本身已经是根协程,此时根协程的 CoroutineContext 也没有携带 CoroutineExceptionHandler,
            // 从而导致了直接异常。
            try {
                scope.launch {
                    throw NullPointerException()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        findViewById<Button>(R.id.button5).setOnClickListener {
            val scope = CoroutineScope(Job())
            //如果根协程或者scope中没有设置 CoroutineExceptionHandler，异常会被直接抛出,所以这里会直接导致闪退
            scope.launch {
                val asyncA = async(SupervisorJob() + CoroutineExceptionHandler { _, t ->
                    //这里不会执行,异常不会被捕获
                    println("throwable ->  ${t.message}")
                }) {
                    //这里会执行, 但是异常会在await的时候抛出, (使用SupervisorJob或者在根协程执行async)
                    println("a async")
                    throw RuntimeException()
                }
                //异常会在这里被抛出
                asyncA.await()
            }
        }

        //不会闪退
        //当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
        //async或produce作为非根协程的时候, 内部的异常会第一时间抛出
        //async加入SupervisorJob后,异常也不会在内部第一时间抛出,会在await的时候抛出
        findViewById<Button>(R.id.button6).setOnClickListener {
            val scope = CoroutineScope(Job())
            scope.launch {
                //async加入SupervisorJob后,异常也不会在内部第一时间抛出,会在await的时候抛出
                val asyncA = async(SupervisorJob()) {
                    println("async A")
                    throw RuntimeException()
                }
                val asyncB = async(SupervisorJob()) {
                    println("async B")
                    throw RuntimeException()
                }

                val resultA = kotlin.runCatching {
                    println("A await")
                    asyncA.await()
                }
                val resultB = kotlin.runCatching {
                    println("B await")
                    asyncB.await()
                }

                println("resultA=$resultA")
                println("resultB=$resultB")
            }
        }

        //会闪退
        //当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
        //async或produce作为非根协程的时候, 内部的异常会第一时间抛出
        findViewById<Button>(R.id.button7).setOnClickListener {
            val scope = CoroutineScope(Job())
            scope.launch {
                //try不到
                try {
                    //非根协程会立即抛出异常
                    val asyncA = async() {
                        throw RuntimeException()
                    }
                    val asyncB = async() {
                        throw RuntimeException()
                    }

                    kotlin.runCatching {
                        asyncA.await()
                    }
                    kotlin.runCatching {
                        asyncB.await()
                    }
                } catch (e: Exception) {
                }
            }
        }

        //不会闪退
        //1.当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
        //2.async或produce作为非根协程的时候, 内部的异常会第一时间抛出
        //3.async为非根协程,加入SupervisorJob后,异常也不会在内部第一时间抛出,会在await的时候抛出
        //4.async为非根协程,外部用supervisorScope套一层之后,异常也不会在内部第一时间抛出,会在await的时候抛出
        findViewById<Button>(R.id.button8).setOnClickListener {
            val scope = CoroutineScope(Job())
            scope.launch {
                supervisorScope {
                    val asyncA = async() {
                        println("async A")
                        throw RuntimeException()
                    }
                    val asyncB = async() {
                        println("async B")
                        throw RuntimeException()
                    }

                    /*val resultA = kotlin.runCatching {
                        println("A await")
                        asyncA.await()
                    }
                    val resultB = kotlin.runCatching {
                        println("B await")
                        asyncB.await()
                    }

                    println("resultA=$resultA")
                    println("resultB=$resultB")*/
                }

            }
        }

        //会闪退
        //1.当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
        //2.async或produce作为非根协程的时候, 内部的异常会第一时间抛出
        //3.async为非根协程,加入SupervisorJob后,异常也不会在内部第一时间抛出,会在await的时候抛出
        //4.async为非根协程,外部用supervisorScope套一层之后,异常也不会在内部第一时间抛出,会在await的时候抛出
        //5.async为非根协程,外部用coroutineScope套一层之后,异常会在内部第一时间抛出,不会在await的时候抛出
        //6.就算根协程使用的是SupervisorJob, 但是在启动async的时候是非根协程, async也会直接抛出异常,而不是在await处抛出
        findViewById<Button>(R.id.button9).setOnClickListener {
            val scope = CoroutineScope(Job())
            scope.launch {
                coroutineScope {
                    val asyncA = async() {
                        println("async A")
                        //会直接在这里抛出异常,而不是在await的时候排除
                        throw RuntimeException()
                    }
                    val asyncB = async() {
                        println("async B")
                        throw RuntimeException()
                    }

                }

            }
        }

        //会闪退
        //6.就算根协程使用的是SupervisorJob, 但是在启动async的时候是非根协程, async也会直接抛出异常,而不是在await处抛出
        findViewById<Button>(R.id.button10).setOnClickListener {
            lifecycleScope.launch {
                val asyncA = async() {
                    println("async A")
                    //会直接在这里抛出异常,而不是在await的时候排除
                    throw RuntimeException()
                }
                val asyncB = async() {
                    println("async B")
                    throw RuntimeException()
                }


            }
        }

        //会闪退
        findViewById<Button>(R.id.button11).setOnClickListener {
            lifecycleScope.launch {
                //这里捕获不到异常
                try {
                    val asyncA = async() {
                        println("async A")
                        //会直接在这里抛出异常,而不是在await的时候排除
                        throw RuntimeException()
                    }
                    val asyncB = async() {
                        println("async B")
                        throw RuntimeException()
                    }
                } catch (e: Exception) {
                    println(e)
                }


            }
        }

        //不会闪退
        findViewById<Button>(R.id.button12).setOnClickListener {
            lifecycleScope.launch {
                //这里可以捕获到异常
                try {
                    coroutineScope {
                        val asyncA = async() {
                            println("async A")
                            //会直接在这里抛出异常,而不是在await的时候排除
                            throw RuntimeException()
                        }
                        val asyncB = async() {
                            println("async B")
                            throw RuntimeException()
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }


            }
        }

        //不会闪退
        findViewById<Button>(R.id.button13).setOnClickListener {
            lifecycleScope.launch {
                //可以捕获到asyncA.await()抛出的异常

                supervisorScope {
                    val asyncA = async() {
                        println("async A")
                        //外面使用supervisorScope后这里不直接抛出异常,而是在await的时候
                        throw RuntimeException()
                    }
                    val asyncB = async() {
                        println("async B")
                        throw RuntimeException()
                    }
                    try {
                        //会在这里抛出异常
                        asyncA.await()

                        asyncB.await()
                    } catch (e: Exception) {
                        println(e)
                    }
                }

            }
        }

        //不会闪退
        findViewById<Button>(R.id.button14).setOnClickListener {
            val scope = CoroutineScope(Job() + CoroutineExceptionHandler { coroutineContext, throwable ->
                    //这里收不到异常
                    println("1 throwable=$throwable")
                })
            scope.launch {
                //当子协程异常时,因为我们使用了 supervisorScope ,所以异常此时不会主动传递给外部，而是由子类自行处理。
                supervisorScope {
                    launch(CoroutineName("A") + CoroutineExceptionHandler { coroutineContext, throwable ->
                        //可以捕获到异常
                        println("2 throwable=$throwable")
                    }) {
                        delay(1000)
                        throw RuntimeException()
                    }
                    launch(CoroutineName("B")) {
                        delay(1500)

                        //可以正常执行
                        println("---->B")
                    }
                }

            }
        }

        //不会闪退
        //coroutineScope其主要用于并行分解协程子任务时而使用
        //当其范围内任何子协程失败时，其所有的子协程也都将被取消，一旦内部所有的子协程完成，其也会正常返回。
        //当子协程异常时,因为我们使用了 coroutineScope ,所以当其范围内任何子协程失败时，其所有的子协程也都将被取消
        //当子协程A 异常未被捕获时，此时 子协程B 和整个 协程作用域 都将被异常取消，如果外层没有try的话,此时异常将传递到顶级 CoroutineExceptionHandler 。
        findViewById<Button>(R.id.button15).setOnClickListener {
            val scope = CoroutineScope(Job() + CoroutineExceptionHandler { coroutineContext, throwable ->
                    //这里会收到异常
                    println("1 throwable=$throwable")
                })
            scope.launch {
                //当子协程异常时,因为我们使用了 coroutineScope ,所以当其范围内任何子协程失败时，其所有的子协程也都将被取消
                //当子协程A 异常未被捕获时，此时 子协程B 和整个 协程作用域 都将被异常取消，此时异常将传递到顶级 CoroutineExceptionHandler 。
                coroutineScope {
                    launch(CoroutineName("A") + CoroutineExceptionHandler { coroutineContext, throwable ->
                        //不会捕获到异常,除非使用supervisorJob
                        println("2 throwable=$throwable")
                    }) {
                        delay(1000)
                        throw RuntimeException()
                    }
                    launch(CoroutineName("B")) {
                        delay(1500)

                        //不能正常执行, 协程B异常的时候会传递到父协程,从而让B取消
                        println("---->B")
                    }
                }

            }
        }

        //不会闪退
        //coroutineScope其主要用于并行分解协程子任务时而使用
        //当其范围内任何子协程失败时，其所有的子协程也都将被取消，一旦内部所有的子协程完成，其也会正常返回。
        findViewById<Button>(R.id.button16).setOnClickListener {
            val scope = CoroutineScope(Job() + CoroutineExceptionHandler { coroutineContext, throwable ->
                //这里会收到异常
                println("1 throwable=$throwable")
            })
            scope.launch {
                //当子协程异常时,因为我们使用了 coroutineScope ,所以当其范围内任何子协程失败时，其所有的子协程也都将被取消
                //当子协程A 异常未被捕获时，此时 子协程B 和整个 协程作用域 都将被异常取消，如果外层没有try的话,此时异常将传递到顶级 CoroutineExceptionHandler 。
                coroutineScope {
                    launch(SupervisorJob() + CoroutineName("A") + CoroutineExceptionHandler { coroutineContext, throwable ->
                        //可以捕获到异常,使用了supervisorJob
                        println("2 throwable=$throwable")
                    }) {
                        delay(1000)
                        throw RuntimeException()
                    }
                    launch(CoroutineName("B")) {
                        delay(1500)

                        //正常执行, 协程A使用SupervisorJob来启动,自己处理了异常不会导致这里取消
                        println("---->B")
                    }
                }
            }
        }


        //不会闪退
        //当子协程A 异常未被捕获时，此时 子协程B 和整个 协程作用域 都将被异常取消，如果外层没有try的话,此时异常将传递到顶级 CoroutineExceptionHandler 。
        findViewById<Button>(R.id.button17).setOnClickListener {
            val scope = CoroutineScope(Job() + CoroutineExceptionHandler { coroutineContext, throwable ->
                //这里会收到异常
                println("1 throwable=$throwable")
            })
            scope.launch {
                //可以try-catch到
                //当子协程异常时,因为我们使用了 coroutineScope ,所以当其范围内任何子协程失败时，其所有的子协程也都将被取消
                //当子协程A 异常未被捕获时，此时 子协程B 和整个 协程作用域 都将被异常取消，如果外层没有try的话,此时异常将传递到顶级 CoroutineExceptionHandler 。
                try {
                    coroutineScope {
                        launch(CoroutineName("A")) {
                            delay(1000)
                            throw RuntimeException()
                        }
                        launch(CoroutineName("B")) {
                            delay(1500)

                            //不能正常执行, 协程B异常的时候会传递到父协程,从而让B取消
                            println("---->B")
                        }
                    }
                } catch (e: Exception) {
                    //这里可以被try到
                    println("try->"+e)
                }

            }
        }
    }
}