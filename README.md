# 协程知识点总结

## 1.常用挂起函数(suspendCancellableCoroutine、withContext、coroutineScope)

### 1.launch,async

协程内部使用launch或者async函数会启动一个新的协程，并不是挂起函数，所以后面的代码还是会继续执行，"end"会在"2"之前打印出来, 除非使用join或者await

```
/*
[main]->start
[main]->end
[DefaultDispatcher-worker-1]->1
[DefaultDispatcher-worker-1]->2
 */
btn1.setOnClickListener {
    lifecycleScope.launch {
        log("start")
        val a = launch(Dispatchers.Default) {
            log("1")
            delay(1000)
            log("2")
        }
        log("end")
    }
}

/*
[main]->start
[main]->end
[main]->1
[main]->2
 */
btn2.setOnClickListener {
    lifecycleScope.launch {
        log("start")
        val a = launch {
            log("1")
            delay(1000)
            log("2")
        }
        log("end")
    }
}
```

### 2.withContext

不会创建新的协程，在指定协程上运行挂起代码块，并挂起该协程直至代码块运行完成。

suspendCancellableCoroutine、withContext、coroutineScope的具体区别看下面。

```
/*                                                                               
[main]->start                                                                    
[DefaultDispatcher-worker-1]->withContext start                                  
[main]->result=zj                                                                
[main]->end                                                                      
 */                                                                              
btn1.setOnClickListener {                                                         
    lifecycleScope.launch {                                                       
        log("start")                                                             
                                                                                 
        val result = withContext(Dispatchers.IO) {                                
            log("withContext start")                                             
            delay(1000)                                                          
            return@withContext "zj"                                              
        }                                                                        
        log("result=$result")                                                    
        log("end")                                                               
    }                                                                            
}                                                                                
```



### 3.suspendCoroutine和suspendCancellableCoroutine

这两个方法是挂起函数，它并不是帮我们启动协程的，它运行在协程当中并且帮我们获取到当前协程的 Continuation 实例，也就是拿到回调，方便后面我们调用它的 resume 或者 resumeWithException 来返回结果或者抛出异常。如果你重复调用 resume 或者 resumeWithException 会出现异常IllegalStateException.

在 Kotlin 协程库中，有很多协程的构造器方法，这些构造器方法内部可以使用挂起函数来封装回调的 API。
最主要的 API 是 suspendCoroutine() 和 suspendCancellableCoroutine()就是这类挂起函数，后者是可以被取消的。

注意点：必须在block代码块中使用resume或者resumeWithException，否则该函数会一直挂起，后续代码将无法调用。

```
suspend fun getUserCoroutine(): String = suspendCoroutine<String> { continuation->
    getUser {
        log("continuation resume $it")
        continuation.resume(it)
    }
}
//耗时函数
fun getUser(callBack: Callback) {
        thread(isDaemon = true) {
            Thread.sleep(1000)
            callBack.invoke("zj")
        }
}

```



### 4.使用区别

#### 1.相同点

suspendCancellableCoroutine、withContext、coroutineScope的相同点是，他们都是挂起函数，都可以有返回值。

#### 2.不同点

withContext可以用来切换线程，但是不能封装回调api，而suspendCancellableCoroutine是用来封装回调api，使用resume 或者 resumeWithException 来返回结果或者抛出异常，只是不能切换线程，但是可以在内部启动线程来切换线程，还有一个重要区别是传给它的block参数并不是一个挂起函数，只是个普通函数（适合用来封装回调api），而传递给withContext和coroutineScope的block参数都是挂起函数，所以suspendCancellableCoroutine的block内部不能使用delay这类挂起函数，而withContext和coroutineScope可以使用delay等挂起函数。suspendCancellableCoroutine还具有一个invokeOnCancellation来监听协程是否取消。

#### 3.使用场景

在非挂起函数需要封装成协程来使用的时候，比如okhttp等网络请求，view的事件回调等可以使用suspendCancellableCoroutine，

withContext和coroutineScope非常类似，在协程中需要切换线程的时候使用withContext， 不切换线程的时候使用coroutineScope

3个方法的定义如下

```
public suspend inline fun <T> suspendCancellableCoroutine(
    crossinline block: (CancellableContinuation<T>) -> Unit
): T
```

```
public suspend fun <T> withContext(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T
```

```
public suspend fun <R> coroutineScope(
		block: suspend CoroutineScope.() -> R
): R
```

#### 4.具体用例

##### 1.等待 View 布局完成后，获取布局大小

封装了一个等待 View 传递下一次布局事件的任务 (比如说，我们改变了一个 TextView 中的内容，需要等待布局事件完成后才能获取该控件的新尺寸):

```
suspend fun View.awaitNextLayout() = suspendCancellableCoroutine<Unit> { cont ->
    // 这里的 lambda 表达式会被立即调用，允许我们创建一个监听器
    val listener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(v: View?,left: Int, top: Int,right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            // 视图的下一次布局任务被调用
            // 先移除监听，防止协程泄漏
            removeOnLayoutChangeListener(this)
            
            // 这里如果resume不调用协程会一直挂起，知道onLayoutChange被回调然后onResume被调用
            // 最终，唤醒协程，恢复执行
            cont.resume(Unit)
        }
    }
    // 如果协程被取消，移除该监听
    cont.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
  
    addOnLayoutChangeListener(listener)

    // 这样协程就被挂起了，除非监听器中的 cont.resume() 方法被调用
}
```

然后在activity中调用:

```
lifecycleScope.launch {
    tvTitle.visibility = View.GONE
    tvTitle.text = ""

    //1->tvTitle.width=0
    println("1->tvTitle.width=" + tvTitle.width)
    // 等待下一次布局事件的任务，然后才可以获取该视图的高度
    tvTitle.awaitNextLayout()

    //2->tvTitle.width=258
    println("2->tvTitle.width=" + tvTitle.width)

    // 布局任务被执行
    // 现在，我们可以将视图设置为可见，并其向上平移，然后执行向下的动画
    tvTitle.visibility = View.VISIBLE
    tvTitle.translationX = -tvTitle.width.toFloat()
    tvTitle.animate().translationY(0f)
}

btn2.setOnClickListener {
    // 将该视图设置为可见，再设置一些文字
    tvTitle.visibility = View.VISIBLE
    tvTitle.text = "Hi everyone!"
}
```