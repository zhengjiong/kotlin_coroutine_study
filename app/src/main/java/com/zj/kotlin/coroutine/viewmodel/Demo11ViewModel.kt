package com.zj.kotlin.coroutine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
 *
 * CreateTime:2022/4/30 20:01
 * @author zhengjiong
 */
class Demo11ViewModel : ViewModel() {
    val coroutineScopeTest =
        CoroutineScope(Job() + Dispatchers.Main.immediate + CoroutineName("coroutineScope"))

    val supervisorScopeTest =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("coroutineScope"))

    private val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        println("exceptionHandler->throwable=" + throwable.message + " |thread->" + Thread.currentThread().name)
    }

    suspend fun exceptionTest(): String {
        return suspendCancellableCoroutine {
            it.invokeOnCancellation {
                println("invokeOnCancellation  |thread->" + Thread.currentThread().name)
            }
            thread {
                try {
                    Thread.sleep(3000)
                    throw NullPointerException("execeptionTest")
                } catch (e: Exception) {
                    it.resumeWithException(e)
                }
            }
        }
    }

    suspend fun successTest(): String {
        return suspendCancellableCoroutine<String> {
            it.invokeOnCancellation {
                println("successTest -> invokeOnCancellation")
            }
            thread {
                Thread.sleep(3000)
                it.resume("success")
            }
        }
    }

    /**
     * app crash
     * 这里是try不到的
     */
    fun test1() {
        try {
            viewModelScope.launch {
                exceptionTest()
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

    /**
     * 可以catch到
     */
    fun test2() {
        viewModelScope.launch {
            try {
                exceptionTest()
            } catch (e: Exception) {
                println("test2 catch-> " + e.message)
            }
        }
    }

    /**
     * 把异常放到CoroutineExceptionHandler去处理
     */
    fun test3() {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            exceptionTest()
        }
    }

    /**
     * 一个协程出异常另外一个也会被终止
     *
     * exceptionTest会抛出异常,从而
     * successTest的执行也会被终止(注意:不是取消,不会执行invokeOnCancellation方法)
     */
    fun test4() {
        viewModelScope.launch() {
            try {
                exceptionTest()
                val result = successTest()
                println("result=$result")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 分开try, successTest不会受到影响
     */
    fun test5() {
        viewModelScope.launch() {
            try {
                exceptionTest()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val result = successTest()
                println("result->$result")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 1.父作用域的生命周期持续到所有子作用域执行完；

     * 2.当结束父作用域结束时，同时结束它的各个子作用域；

     * 3.子作用域未捕获到的异常将不会被重新抛出，而是一级一级向父作用域传递，这种异常传播将导致父父作用域失败，进而导致其子作用域的所有请求被取消。

     * 这样是try不到的
     */
    fun test6() {
        viewModelScope.launch(Dispatchers.Default) {
            try {//子协程
                launch {
                    exceptionTest()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 可以被try
     */
    fun test7() {
        viewModelScope.launch(Dispatchers.Default) {
            //子协程
            launch {
                try {
                    exceptionTest()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 不会crash
     * async或produce作为根协程的时候, 内部的异常不会第一时间抛出,
     * 而是依赖用户最终消费异常:await或者receive
     */
    fun test8() {
        coroutineScopeTest.async {
            delay(1000)
            println("ArithmeticException()")
            throw ArithmeticException()
        }

    }

    /**
     * 不会crash
     * async或produce作为根协程的时候, 内部的异常不会第一时间抛出,
     * 而是依赖用户最终消费异常:await或者receive
     */
    fun test8_1() {
        supervisorScopeTest.async {
            delay(1000)
            println("ArithmeticException()")
            throw ArithmeticException()
        }

    }

    /**
     * 会crash
     * async或produce作为非根协程的时候, 内部的异常会第一时间抛出
     *
     */
    fun test8_2() {
        coroutineScopeTest.launch {
            //非根协程
            async {
                delay(1000)
                println("ArithmeticException()")
                throw ArithmeticException()
            }

        }

    }

    /**
     * 会crash
     * 当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
     * async或produce作为非根协程的时候, 内部的异常会第一时间抛出
     */
    fun test8_3() {
        supervisorScopeTest.launch {
            //非根协程
            async {
                delay(1000)
                println("ArithmeticException()")
                throw ArithmeticException()
            }

        }
    }

    /**
     * 会crash
     * async或produce作为非根协程的时候, 内部的异常会第一时间抛出
     */
    fun test8_31() {
        supervisorScopeTest.launch {
            //非根协程
            //try不到
            try {
                /*改成withContext就可以try*/
                async {
                    delay(1000)
                    println("ArithmeticException()")
                    throw ArithmeticException()
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    /**
     * 不会crash,会被exceptionHandler捕获异常
     * async或produce作为非根协程的时候, 内部的异常会第一时间抛出
     * 这里用supervisorScopeTest或coroutineScopeTest结果都一样
     */
    fun test8_4() {
        supervisorScopeTest.launch(exceptionHandler) {
            //非根协程
            //try不到
            async {
                delay(1000)
                println("ArithmeticException()")
                throw ArithmeticException()
            }

        }
    }

    //会crash
    fun test8_5() {
        supervisorScopeTest.launch {
            //非根协程
            //try不到
            try {
                async {
                    delay(1000)
                    println("ArithmeticException()")
                    throw ArithmeticException("ArithmeticException")
                }
            } catch (e: Exception) {
                println(e.message)
            }

        }
    }

    //会crash
    fun test8_6() {
        coroutineScopeTest.launch {
            //非根协程
            //try不到
            try {
                async {
                    delay(1000)
                    println("ArithmeticException()")
                    throw ArithmeticException("ArithmeticException")
                }
            } catch (e: Exception) {
                println(e.message)
            }

        }
    }

    //不会crash, 会被exceptionHandler捕获
    //这里用supervisorScopeTest或coroutineScopeTest结果都一样
    fun test8_7() {
        coroutineScopeTest.launch(exceptionHandler) {
            //非根协程
            //try不到
            try {
                async {
                    delay(1000)
                    println("ArithmeticException()")
                    throw ArithmeticException("ArithmeticException")
                }
            } catch (e: Exception) {
                println(e.message)
            }

        }
    }

    /**
     * async或produce作为非根协程的时候, 内部产生异常会第一时间抛出,
     * 程序会crash
     */
    fun test9() {
        coroutineScopeTest.launch {
            async {
                delay(1000)
                println("ArithmeticException()")
                throw ArithmeticException()
            }

        }

    }

    /**
     * async或produce作为非根协程的时候, 内部产生异常会第一时间抛出(无论supervisorScope,
     * 还是CoroutineScope)
     *
     * 程序会crash
     */
    fun test10() {
        viewModelScope.launch {
            //try不到
            try {
                async {
                    delay(1000)
                    println("ArithmeticException()")
                    throw ArithmeticException()
                }
            } catch (e: Exception) {
                println(e.message)
            }
            delay(3000)
            println("viewModelScope end")
        }

    }

    /**
     * 不会crash
     *
     * 当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
     *
     * 另外要注意我们使用了 supervisorScope{...}调用async和await，如前文所述，SupervisorJob 会让协程自己处理异常，
     * 相应的，如果我们在这里使用Job ，异常将自动在协程层次结构中传播，所以catch中的代码不会被调用(看test11_1)
     */
    fun test11() {
        viewModelScope.launch {
            //这里必须使用supervisorScope
            supervisorScope {
                //当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
                val deferred = async {
                    delay(1000)
                    println("throw ArithmeticException()")
                    throw ArithmeticException()
                }
                //可以被catch
                try {
                    deferred.await()
                } catch (e: Exception) {
                    println("catch $e")
                }
            }
            println("viewModelScope 2")
            delay(3000)
            println("viewModelScope end")
        }

    }

    /**
     *
     * 会crash
     *
     * 当async在coroutineScope中作为根协程启动时, 也会直接抛出异常,异常将自动在协程层次结构中传播，所以catch中的代码不会被调用
     *
     */
    fun test11_1() {
        GlobalScope.launch {
            //要避免crash,可以在这里加一个try-catch, 具体看test11_1_1
            coroutineScope {
                //当async在coroutineScope中的直接启动时, 也会直接抛出异常,相当于不是作为根协程直接启动
                //try不到, 如果执行.await可以被try到,但是程序依然会crash, 因为已经被抛到父协程了
                try {
                    async {
                        delay(1000)
                        println("throw 1()")
                        throw ArithmeticException()
                    }
                    //这里如果执行.await就可以被下面catch1 catch到,但是程序异常会crash,
                    //如果想不crash 需要在coroutineScope外层再加一个try,具体看test11_1_1,
                } catch (e: Exception) {
                    //catch不到
                    println("catch1---->$e")
                }
            }
        }

    }

    /**
     * 输出:
     * ---------------------
     * throw ArithmeticException()
     * catch2---->java.lang.ArithmeticException
     *
     * 不会crash
     *
     * 当async在coroutineScope中作为根协程启动时, 也会直接抛出异常,
     *
     * 如果我们在这里使用Job(coroutineScope)，异常将自动在协程层次结构中传播，所以catch中的代码不会被调用
     */
    fun test11_1_1() {
        GlobalScope.launch {
            try {
                coroutineScope {
                    //当async在coroutineScope中的直接启动时, 也会直接抛出异常,相当于不是作为根协程直接启动
                    //try不到
                    try {
                        async {
                            delay(1000)
                            println("throw ArithmeticException()")
                            throw ArithmeticException()
                        }
                    } catch (e: Exception) {
                        //catch不到
                        //catch不到是因为没执行.await
                        println("catch1---->$e")
                    }
                    //会被打印, 因为async是并发执行的,并不是挂起函数,所以会执行到这里, 一旦
                    //执行到上面的throw ArithmeticException(),就会立即抛出异常, 内部的try-catch不会捕获到, 但是外面的可以
                    println("---------------------")
                }
            } catch (e: Exception) {
                //可以被catch到
                println("catch2---->$e")
            }
        }

    }

    /**
     * 输出:
     * ---------------------
     * throw ArithmeticException()
     * catch2---->java.lang.ArithmeticException
     *
     * 不会crash
     *
     * 当async在coroutineScope中作为根协程启动时, 也会直接抛出异常,
     *
     * 如果我们在这里使用Job(coroutineScope)，异常将自动在协程层次结构中传播，所以catch中的代码不会被调用
     */
    fun test11_1_2() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    //async作为根协程被启动,不会立即抛出异常,在await的时候抛出
                    try {
                        val deferred = supervisorScopeTest.async {
                            delay(1000)
                            println("throw ArithmeticException()")
                            throw ArithmeticException()
                        }

                        try {
                            deferred.await()
                        } catch (e: Exception) {
                            //可以被捕获到:
                            //catch1---->java.lang.ArithmeticException

                            //但是第二次执行该方法会捕获到这个exception:
                            //catch1---->kotlinx.coroutines.JobCancellationException: Parent job is Cancelled; job=JobImpl{Cancelled}@9e14a3c
                            //应该是使用coroutineScopeTest的问题, 如果换成supervisorScope就没有这个问题了,每次执行都会捕获到catch1---->java.lang.ArithmeticException
                            println("catch1---->$e")
                        }

                    } catch (e: Exception) {
                        //catch不到
                        println("catch2---->$e")
                    }
                    //会被打印, 因为async是并发执行的,并不是挂起函数,所以会执行到这里, 一旦
                    //执行到上面的throw ArithmeticException(),就会立即抛出异常, 内部的try-catch不会捕获到, 但是外面的可以
                    println("---------------------")
                }
            } catch (e: Exception) {
                //可以被catch到
                println("catch3---->$e")
            }
        }

    }

    /**
     * 不会crash
     * 可以被try-catch到
     *
     *
     */
    fun test11_2() {
        viewModelScope.launch {
            //这里必须使用supervisorScope
            coroutineScope {
                //当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
                val deferred = supervisorScopeTest.async {
                    delay(1000)
                    println("throw ArithmeticException()")
                    throw ArithmeticException()
                }

                //可以被try-catch到
                try {
                    deferred.await()
                } catch (e: Exception) {
                    println("catch $e")
                }
            }
        }

    }

    /**
     * 不会crash
     *
     * 可以被try-catch
     * 这里虽然不会crash但是多次调用该方法只会输出一次 println("throw ArithmeticException()")
     * coroutineScopeTest.async换成supervisorScopeTest.async就可以多次输出,看test11_4
     *
     * 多次执行该方法会报这个错误:
     * kotlinx.coroutines.JobCancellationException: Parent job is Cancelled; job=JobImpl{Cancelled}@7674a12
     */
    fun test11_3() {
        viewModelScope.launch {
            //多次执行该方法,执行到async会报这个错误(换成supervisorScope也是一样):
            //kotlinx.coroutines.JobCancellationException: Parent job is Cancelled; job=JobImpl{Cancelled}@7674a12
            //这里换成supervisorScope,也只会输出一次 println("throw ArithmeticException()"),也会报这个异常
            //是由于coroutineScopeTest已经抛出过异常,导致他的父协程已经取消, 换成supervisorScopeTest就没有这个问题了,看test11_4
            println("1")
            coroutineScope {
                //当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
                println("2")
                //coroutineScopeTest.async启动话就是根协程, 不会主动抛出, 可以被try-catch,
                //如果去掉coroutineScopeTest, 直接async启动,就会立即抛出异常
                val deferred = coroutineScopeTest.async {
                    println("3")
                    delay(1000)
                    println("throw ArithmeticException()")
                    throw ArithmeticException()
                }
                try {
                    //可以被try-catch
                    deferred.await()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

    }

    /**
     * 不会crash
     * 可以被try-catch到
     */
    fun test11_4() {
        viewModelScope.launch {
            coroutineScope {
                //当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
                val deferred = supervisorScopeTest.async {
                    delay(1000)
                    println("throw ArithmeticException()")
                    throw ArithmeticException()
                }
                try {
                    deferred.await()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

    }

    //不会crash
    //当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
    fun test12() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    //async作为根协程被启动, 异常会在await的时候抛出
                    val deferred = async {
                        delay(1000)
                        println("throw ArithmeticException()")
                        throw ArithmeticException("ArithmeticException")
                    }
                }
            } catch (e: Exception) {
                println("catch e=" + e.message)
            }
            println("viewModelScope 2")
            delay(3000)
            println("viewModelScope end")
        }

    }

    //不会crash
    fun test12_1() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    //async作为根协程被启动, 异常会在await的时候抛出
                    val deferred = async {
                        delay(1000)
                        println("throw ArithmeticException()")
                        throw ArithmeticException("ArithmeticException")
                    }
                }
            } catch (e: Exception) {
                println("catch e=" + e.message)
            }
            println("viewModelScope 2")
            delay(3000)
            println("viewModelScope end")
        }

    }

    //会crash,exceptionHandler无法捕获被作为跟协程启动的async
    fun test13() {
        viewModelScope.launch {
            /**
             * 这里的async作为根协程被启动,exceptionHandler是无法捕获的,
             * async作为子协程被启动才能被exceptionHandler捕获
             */
            val deferred = coroutineScopeTest.async(exceptionHandler) {

                delay(1000)
                println("throw ArithmeticException")
                throw ArithmeticException("ArithmeticException")
            }
            deferred.await()
        }
    }

    //不会crash, async作为根协程被启动, 可以被try-catch捕获
    fun test14() {
        viewModelScope.launch {
            /**
             * 这里的async作为根协程被启动
             */
            val deferred = coroutineScopeTest.async {

                delay(1000)
                println("throw ArithmeticException")
                throw ArithmeticException("ArithmeticException")
            }

            try {
                deferred.await()
            } catch (e: Exception) {
                println("catch $e")
            }
        }
    }

    //不会crash,异常会被exceptionHandler捕获
    fun test15() {
        viewModelScope.launch(exceptionHandler) {
            /**
             * 下面被注释掉的exceptionHandler是不能捕获的会导致crash
             * 这里的async作为子协程被启动,发生异常会被第一时间抛出,
             * 所以不能在这里加exceptionHandler, 要在父协程加exceptionHandler
             */
            val deferred = async(/*exceptionHandler*/) {

                delay(1000)
                println("throw ArithmeticException")
                throw ArithmeticException("ArithmeticException")
            }
            println("start--->await")
            deferred.await()
            println("end--->await")
        }
    }

    //可以被catch捕获到, 但是还是会crash
    //因为async作为非根协程被启动,会直接抛出异常,从而导致父协程异常
    fun test16() {
        coroutineScopeTest.launch {
            try {
                //async作为非根协程被启动, 会立即抛出异常, 外层trycatch可以捕获到,但是也会传播到父协程导致crash
                val deferred = async {
                    println("1")
                    delay(500)
                    //非根协程会被立即抛出
                    //只有在这里try, 才不会被抛出
                    throw KotlinNullPointerException()
                }

                //这里不执行await也会导致抛出异常
                deferred.await()
            } catch (e: Exception) {
                //注意这里异常会被捕获, 但是app还是会crash, 因为异常已经被抛出到父协程了
                println("catch------>$e")
            }
        }
    }

    //可以被catch1捕获到, 但是还是会crash
    //因为async作为非根协程被启动,会直接抛出异常, 从而导致父协程异常
    //结果和test16()一样
    fun test17() {
        try {
            supervisorScopeTest.launch {
                //可以捕获,但是还是会导致父协程异常
                try {
                    val deferred = async {
                        println("1")
                        delay(500)
                        //非根协程会被立即抛出
                        //只有在这里try, 才不会被抛出
                        throw KotlinNullPointerException()
                    }


                    //这里不执行await不会导致
                    deferred.await()
                } catch (e: Exception) {
                    println("catch1------>$e")
                }
            }
        } catch (e: Exception) {
            println("catch2------->$e")
        }
    }

    /**
     * 当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
     * 可以被try到,不会crash
     */
    fun test18() {
        coroutineScopeTest.launch {
            val deferred = coroutineScopeTest.async {
                println("1")
                delay(500)
                throw KotlinNullPointerException()
            }

            try {
                //可以被try到
                deferred.await()
            } catch (e: Exception) {
                println("catch------>$e")
            }
        }
    }

    /**
     * 当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
     * 可以被try到,不会crash
     * 结果和test18()一样
     */
    fun test19() {
        coroutineScopeTest.launch {
            val deferred = supervisorScopeTest.async {
                println("1")
                delay(500)
                throw KotlinNullPointerException()
            }

            try {
                //这里不执行await不会导致
                deferred.await()
            } catch (e: Exception) {
                println("catch------>$e")
            }
        }
    }

    /**
     * 当async被用作构建根协程（由协程作用域直接管理的协程）时，异常不会主动抛出，而是在调用.await()时抛出。
     * 可以被try到,不会crash
     * 结果和test19()一样
     */
    fun test20() {
        supervisorScopeTest.launch {
            val deferred = supervisorScopeTest.async {
                println("1")
                delay(500)
                throw KotlinNullPointerException()
            }

            try {
                //这里不执行await不会导致
                deferred.await()
            } catch (e: Exception) {
                println("catch------>$e")
            }
        }
    }
}