package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_demo10.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 *
 * //外面套了supervisorScope, async不会第一时间抛出异常而是等到await的时候
 * //外面套了coroutineScope, async还是会第一时间抛出异常
 * //async使用SupervisorJob()来启动, async不会第一时间抛出异常而是等到await的时候
 * //async作为根协程来启动, async不会第一时间抛出异常而是等到await的时候
 *
 * CreateTime:2020/8/17 13:29
 * @author zhengjiong
 */
class Demo10Activity : AppCompatActivity() {
    val mainScope = MainScope()

    val jobScope = MyScope(Job() + CoroutineName("my-job-scope") + Dispatchers.Main)
    val supervisorScope =
        MyScope(SupervisorJob() + CoroutineName("my-supervisor-scope") + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo10)
        lifecycleScope.launch {

        }
        /*
            输出:
            1
            KotlinNullPointerException
            app崩溃
         */
        btn1.setOnClickListener {
            jobScope.launch {
                /*这里面如果加try, 不会导致崩溃, 并且可以被try到*/
                println("1")
                throw KotlinNullPointerException()
            }
            jobScope.launch {
                delay(1000)
                println("2")
            }
        }

        /*
            android中app直接crash导致2不会输出, 在jvm的测试中2是可以输出的
            输出:
            1
            KotlinNullPointerException
            app崩溃
         */
        btn2.setOnClickListener {
            //在这里加try-catch是没有用的
            supervisorScope.launch {
                println("1")
                throw KotlinNullPointerException()
            }
            supervisorScope.launch {
                delay(1000)
                println("2")
            }
        }

        /*
            android中app直接crash导致2不会输出, 在jvm的测试中2是可以输出的
            输出:
            1
            KotlinNullPointerException
            app崩溃
         */
        btn3.setOnClickListener {
            jobScope.launch {
                //这里加try-catch是捕获不到异常的, 除非把supervisorScope换成coroutineScope, 异常才会被正常抛出, 不然子协程会自己处理异常
                supervisorScope {
                    //这里加try-catch是捕获不到异常的
                    launch {
                        //这里面可以try
                        //child 1
                        println("1")
                        throw KotlinNullPointerException()
                    }

                    launch {
                        delay(1000)
                        println(2)
                    }

                }
            }
        }

        //app不会crash
        //输出:
        //child-1
        //child-2
        //KotlinNullPointerException
        btn4.setOnClickListener {
            //try只能加在coroutineScope或者withContext直接外层, 其他地方会catch不到, 导致应用crash
            jobScope.launch {
                try {
                    //这里用withContext也可以被try-catch
                    //但是不能用supervisorScope, supervisorScope会让子协程自己处理异常就会导致外层catch不到
                    coroutineScope {
                        launch {
                            //child1
                            println("child-1")
                            delay(1000)
                            throw KotlinNullPointerException("exception")
                        }

                        launch {
                            //child2
                            println("child-2")
                            delay(3000)
                            println("end-2")
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
        //app crash
        //输出:
        //child-1
        //child-2
        btn5.setOnClickListener {
            jobScope.launch {
                try {
                    //直接对launch进行try-catch是捕获不到的
                    launch {
                        //child1
                        println("child-1")
                        delay(1000)
                        throw KotlinNullPointerException("exception")
                    }
                } catch (e: Exception) {
                    println(e)
                }
                launch {
                    //child2
                    println("child-2")
                    delay(3000)
                    println("end-2")
                }

            }
        }

        /**
         * app不会crash
         * 输出:
         * 1
         * KotlinNullPointerException
         */
        btn6.setOnClickListener {
            //jobScope.async不是挂起函数可以在这里直接启动, 但是下面的await是挂起函数，必须在协程体重调用
            //所以这里要用jobScope.launch来启动一个协程体
            jobScope.launch {
                //注意这里是使用jobScope.async,而不是直接async, 所以async是作为根协程被启动, 内部的异常会在await的时候被抛出
                //当async被用作构建根协程(jobScope.async)（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
                //如果这里直接用async启动, 不管下面执不执行await, 这里都会抛出异常切下面无法捕捉到导致程序崩溃
                val deferred1 = jobScope.async {
                    println("1")
                    delay(500)
                    if (true) {
                        throw KotlinNullPointerException()
                    }
                    "result"
                }

                try {
                    deferred1.await()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

        /**
         * app不会crash
         *
         * 如果不进行await()的话, 异常永远都不会被抛出
         *
         * 输出:
         * 1
         *
         */
        btn7.setOnClickListener {
            //当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
            val deferred = jobScope.async {
                println("1")
                delay(500)
                if (true) {
                    throw KotlinNullPointerException()
                }
                "result"
            }
        }


        //todo:这里不是太理解, 后面补充(2024-05-13 22:07:04已经理解,会crash的原因是使用了
        // async会导致异常被立即抛出所以外部的scope也出现异常)
        //注意这里异常会被捕获, 但是app还是会crash
        //app crash
        //由于 scope 的直接子协程是 launch，如果 async 中产生了一个异常，这个异常将会被立即抛出。
        //原因是 async (包含一个 Job 在它的 CoroutineContext 中) 会自动传播异常到它的父级 (launch)，
        //这会让异常被立即抛出。
        //输出:
        //1
        //然后crash
        btn8.setOnClickListener {
            //这里就算改成supervisorScope, 运行结果也一样
            jobScope.launch {
                try {
                    //这里外面套一层coroutineScope也可以让其捕捉到
                    //这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
                    //下面无法捕捉到
                    val deferred = async {
                        println("1")
                        delay(500)
                        //非根协程会被立即抛出
                        //只有在这里try, 才不会被抛出
                        throw KotlinNullPointerException()
                    }

                    //这里不执行await也会导致抛出异常,且不被捕获
                    deferred.await()
                } catch (e: Exception) {
                    //注意这里异常会被捕获, 但是app还是会crash
                    println("------>$e")
                    e.printStackTrace()
                }
            }
        }

        //app不会crash
        //会catch到两次异常
        //输出
        // launch
        // 1
        // 1------>kotlin.KotlinNullPointerException
        // 2------>kotlin.KotlinNullPointerException
        btn9.setOnClickListener {
            GlobalScope.launch {
                try {
                    println("launch")
                    coroutineScope {
                        try {
                            //async非根协程启动,会立即抛出异常所以外层可以try到, 然后外层又会继续抛出,所以最外层又能try到第二次异常
                            val deferred = async<Unit> {
                                println("1")
                                delay(500)
                                throw KotlinNullPointerException()
                            }

                            //这里不执行await也会导致抛出异常,且不被捕获
                            deferred.await()
                        } catch (e: Exception) {
                            //可以被catch到
                            println("1------>$e")
                        }
                    }
                } catch (e: Exception) {
                    //可以被catch到
                    println("2------>$e")
                }
            }
        }

        //todo:不太明白,以后补充
        //app不会crash
        //输出
        // 1
        // 1------>KotlinNullPointerException
        btn10.setOnClickListener {
            jobScope.launch {
                try {
                    coroutineScope {
                        val deferred = async<Unit> {
                            println("1")
                            delay(500)
                            throw KotlinNullPointerException()
                        }

                        //这里不执行await也会导致抛出异常,且不被捕获
                        deferred.await()
                    }
                } catch (e: Exception) {
                    println("1------>$e")
                }
            }
        }

        //todo:不太明白,以后补充
        //app不会crash
        //输出
        // 1
        // 1------>KotlinNullPointerException
        btn11.setOnClickListener {
            jobScope.launch {
                try {
                    supervisorScope {
                        val deferred = async<Unit> {
                            println("1")
                            delay(500)
                            throw KotlinNullPointerException()
                        }

                        deferred.await()
                    }
                } catch (e: Exception) {
                    println("1------>$e")
                }
            }
        }

        //app不会crash
        //输出:
        //1
        //KotlinNullPointerException
        btn12.setOnClickListener {
            jobScope.launch {
                try {
                    //去掉这个SupervisorJob()就会导致try不到, 然后crash,
                    //这样写和jobScope.async的效果是一样的,都可以被try到
                    val deferred = async(SupervisorJob()) {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()
                    }

                    deferred.await()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

        //app crash
        //输出:
        //1
        //KotlinNullPointerException
        btn13.setOnClickListener {
            jobScope.launch {
                coroutineScope {
                    try {
                        val deferred = async<Unit> {
                            println("1")
                            delay(500)
                            throw KotlinNullPointerException()

                        }

                        deferred.await()
                    } catch (e: Exception) {
                        println(e)
                    }
                }
            }
        }


        //app crash
        //异常不能被正确捕获
        //输出:
        //1
        //KotlinNullPointerException
        //crash
        btn14.setOnClickListener {
            jobScope.launch {
                //supervisorScope {
                    //外面套了supervisorScope, async不会第一时间抛出异常而是等到await的时候
                    //外面套了coroutineScope, async还是会第一时间抛出异常
                    //async使用SupervisorJob()来启动, async不会第一时间抛出异常而是等到await的时候
                    //async作为根协程来启动, async不会第一时间抛出异常而是等到await的时候
                    val deferred = async(SupervisorJob()) {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()
                    }
                    /*try {
                        deferred.await()
                    } catch (e: Exception) {
                        println(e)
                    }*/
                //}
            }
        }

        //app crash
        //异常不能被捕获
        //输出:
        //1
        btn15.setOnClickListener {
            //supervisorScope.launch { 这样也不能捕获,照样crash,只有async根协程的方式启动才可以,或者是外面套scope的方式(上面btn10或btn11)
            jobScope.launch(SupervisorJob()) {
                try {
                    val deferred = async {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()

                    }

                    deferred.await()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
        /**
         * 输出:
         * loadData
         * launch start
         * async
         * e->java.lang.NullPointerException: null
         */
        btn16.setOnClickListener {
            println("loadData")
            val job: Job = Job()
            val scope = CoroutineScope(Dispatchers.Default + job+ CoroutineExceptionHandler { coroutineContext, throwable ->
                println("0 launch handle $throwable")
            })
            // may throw Exception
            fun doWork(): Deferred<String> = scope.async {
                println("async")
                if (true) {
                    throw NullPointerException("null")
                }
                "aaaa"
            }   // (1)
            fun loadData() = scope.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                //下面可以try到, 就不会进入这里了
                println("1 launch handle $throwable")
            }) {
                try {
                    //这里虽然可以try到,但是没有使用superviseScope会导致scope启动的其他协程被取消掉
                    doWork().await()   // (2)
                } catch (e: Exception) {
                    println("e->$e")
                }
            }
            loadData()
            scope.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                //被取消后接收不到异常信息
                println("2 launch handle $throwable")
            }) {
                println("launch start")
                delay(1000)
                //end不会输出, 因为没有使用superviseScope, doWork中的异常会导致scope启动的其他协程被取消掉
                println("launch end")
            }
        }

        /**
         * loadData
         * async
         * e->java.lang.NullPointerException: null
         * launch start
         * launch end
         */
        btn17.setOnClickListener {
            println("loadData")
            // may throw Exception
            fun doWork(): Deferred<String> = lifecycleScope.async {
                println("async")
                if (true) {
                    throw NullPointerException("null")
                }
                "aaaa"
            }   // (1)
            fun loadData() = lifecycleScope.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                //下面可以try到, 就不会进入这里了
                println("1 launch handle $throwable")
            }) {
                try {
                    //这里虽然可以try到,但是没有使用superviseScope会导致scope启动的其他协程被取消掉
                    doWork().await()   // (2)
                } catch (e: Exception) {
                    println("e->$e")
                }
            }
            loadData()
            lifecycleScope.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                //被取消后接收不到异常信息
                println("2 launch handle $throwable")
            }) {
                println("launch start")
                delay(1000)
                //end会输出, 因为使用superviseScope, doWork中的异常不会导致scope启动的其他协程被取消掉
                println("launch end")
            }
        }

        /**
         * e->java.lang.NullPointerException: null
         * start
         * end
         * coroutineScope包一层async。现在，当 async 内部发生异常时，它将取消在此范围内创建
         * 的所有其他协程，而不会触及外部范围。 (2)可以在异步块内处理异常。
         */
        btn18.setOnClickListener {
            val job: Job = Job()
            val scope = CoroutineScope(Dispatchers.Default + job+ CoroutineExceptionHandler { coroutineContext, throwable ->
                println("0 launch handle $throwable")
            })

            //coroutineScope包一层
            suspend fun doWork(): String = coroutineScope {// (1)
                async {
                    if (true) {
                        throw NullPointerException("null")
                    }
                    "async"
                }.await()
            }

            fun loadData() = scope.launch {// (2)
                try {
                    doWork()
                } catch (e: Exception) {
                    println("e->$e")
                }
            }
            loadData()
            scope.launch {
                println("start")
                delay(1000)
                println("end")
            }
        }
    }



    /**
     * 仿照MainScope的ContextScope来实现MyScope
     */
    class MyScope(override val coroutineContext: CoroutineContext) : CoroutineScope
}