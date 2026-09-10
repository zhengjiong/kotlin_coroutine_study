package com.zj.renwuxian

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * CreateTime:2026/9/5 10:38
 * @author zhengjiong
 */

fun main() {
    val c = Coroutine_15_2_1()
    //c.test1()
    //c.test2()
    //c.test3()
    //c.test4()
    //c.test5()
    //c.test6()
    c.test7()
}

class Coroutine_15_2_1 {

    fun test1() {
        runBlocking {
            val scope = CoroutineScope(Dispatchers.IO)
            var innerJob: Job? = null
            var coroutineScope: CoroutineScope? = null
            val outerJob = scope.launch(Dispatchers.Default) {
                innerJob = coroutineContext[Job]
                coroutineScope = this
                println("coroutineScope: ${this}, ${this.hashCode()}")
            }
            delay(1000)
            println("scope: ${scope}, ${scope.hashCode()}")
            println("outerJob: ${outerJob}, ${outerJob.hashCode()}")
            println("innerJob: ${innerJob}, ${innerJob.hashCode()}")
            println("coroutineScope: ${coroutineScope}, ${coroutineScope.hashCode()}")
            println("outerJob === innerJob: ${outerJob === innerJob}")
            println("outerJob === innerScope: ${outerJob === coroutineScope}")
        }
    }

    fun test2() {
        "abc".launch2 {
            println(this)
        }
    }

    fun String.launch2(block: String.() -> Unit) {
        block("def")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun test3() {
        val scope = CoroutineScope(Dispatchers.IO)
        var innerJob2: Job? = null
        val job = scope.launch {
            innerJob2 = launch {
                delay(100)
            }
        }
        val children = job.children
        println("children count: ${children.count()}")  //1
        println("innerJob2 === children.first(): ${innerJob2 === children.first()}") //true
        println("innerJob2.parent === job: ${innerJob2?.parent === job}")   //true
    }

    fun test4() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val parentJob = scope.coroutineContext[Job]
        var innerJob: Job? = null
        var innerJob2: Job? = null
        var coroutineScope: CoroutineScope? = null
        val job = scope.launch {
            innerJob = coroutineContext[Job]
            coroutineScope = this

            innerJob2 = launch {
                delay(100)
            }
        }
        val children = job.children
        println("children count: ${children.count()}") //1
        println("parentJob: ${parentJob}")          //JobImpl{Active}@555590
        println("job: ${job}")                      //StandaloneCoroutine{Completing}@6d1e7682
        println("coroutineScope: ${coroutineScope}")//StandaloneCoroutine{Completing}@6d1e7682
        println("innerJob: ${innerJob}")            //StandaloneCoroutine{Completing}@6d1e7682
        println("innerJob2: ${innerJob2}")          //StandaloneCoroutine{Active}@424c0bc4
        println("children.first: ${children.first()}")//StandaloneCoroutine{Active}@424c0bc4
        println("innerJob2 === children.first(): ${innerJob2 === children.first()}") //true
        println("innerJob2.parent === job: ${innerJob2?.parent === job}")   //true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun test5() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val parentJob = scope.coroutineContext[Job]
        var innerJob: Job? = null
        var innerJob2: Job? = null
        var coroutineScope: CoroutineScope? = null
        val job = scope.launch {
            innerJob = coroutineContext[Job]
            coroutineScope = this
            innerJob2 = scope.launch {
                delay(100)
            }
            innerJob2?.join()
        }
        val children = job.children
        println("children count: ${children.count()}") //0
        println("job: ${job}")                      //StandaloneCoroutine{Completed}@6d1e7682
        println("coroutineScope: ${coroutineScope}")//StandaloneCoroutine{Completed}@6d1e7682
        println("innerJob: ${innerJob}")            //StandaloneCoroutine{Completed}@6d1e7682
        println("innerJob2: ${innerJob2}")          //StandaloneCoroutine{Active}@424c0bc4
        println("innerJob2.parent === job: ${innerJob2?.parent === job}")   //false
        println("parentJob: ${parentJob}")          //JobImpl{Active}@555590
        println("innerJob2?.parent: ${innerJob2?.parent}")//JobImpl{Active}@555590
        println("job?.parent: ${job.parent}")  //JobImpl{Active}@555590
        println("innerJob?.parent: ${innerJob?.parent}")//JobImpl{Active}@555590
    }

    fun test6() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val parentJob = scope.coroutineContext[Job]
        var innerJob: Job? = null
        var innerJob2: Job? = null
        var coroutineScope: CoroutineScope? = null
        val job = scope.launch {
            innerJob = coroutineContext[Job]
            coroutineScope = this
            innerJob2 = launch(parentJob!!) {
                delay(100)
            }
            innerJob2?.join()
        }
        val children = job.children
        println("children count: ${children.count()}") //0
        println("job: ${job}")                      //StandaloneCoroutine{Completed}@555590
        println("coroutineScope: ${coroutineScope}")//StandaloneCoroutine{Completed}@555590
        println("innerJob: ${innerJob}")            //StandaloneCoroutine{Completed}@555590
        println("innerJob2: ${innerJob2}")          //StandaloneCoroutine{Active}@6d1e7682
        println("innerJob2.parent === job: ${innerJob2?.parent === job}")   //false
        println("parentJob: ${parentJob}")          //JobImpl{Active}@424c0bc4
        println("innerJob2?.parent: ${innerJob2?.parent}")//JobImpl{Active}@424c0bc4
        println("job?.parent: ${job.parent}")  //JobImpl{Active}@424c0bc4
        println("innerJob?.parent: ${innerJob?.parent}")//JobImpl{Active}@424c0bc4
    }

    fun test7() {
        runBlocking {
            val scope = CoroutineScope(EmptyCoroutineContext)
            var innerJob: Job? = null
            val job = scope.launch {
                val customJob = Job()
                var coroutineScope: CoroutineScope? = null
                innerJob = launch(customJob) {
                    val innerJob2 = this.coroutineContext[Job]
                    println("innerJob2: ${innerJob2}")  //StandaloneCoroutine{Active}@6e2bb28
                    coroutineScope = this
                    delay(100)
                }
                println("customJob: ${customJob}")  //JobImpl{Active}@7768baec
                println("innerJob.parent: ${innerJob.parent}")  //JobImpl{Active}@7768baec
                println("innerJob: ${innerJob}")    //StandaloneCoroutine{Active}@6e2bb28
                println("coroutineScope: ${coroutineScope}")//StandaloneCoroutine{Active}@6e2bb28
                delay(100)
            }
            println("job: ${job}")                  //StandaloneCoroutine{Active}@19e1023e
            job.join()
        }
    }
}