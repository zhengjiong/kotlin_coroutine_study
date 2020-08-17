# 协程知识点总结

## 1.CoroutineContext(协程上下文、拦截器、调度器)

调度器和拦截器本质上就是一个协程上下文的实现。

### 1.上下文

`launch` 函数有三个参数(上下文,启动模式,协程体)，第一个参数叫 **上下文**，它的接口类型是 `CoroutineContext`，通常我们见到的上下文的类型是 `CombinedContext` 或者 `EmptyCoroutineContext`，一个表示上下文的组合，另一个表示什么都没有。

`CoroutineContext` 作为一个集合，它的元素就是源码中看到的 `Element`，每一个 `Element` 都有一个 `key`，因此它可以作为元素出现，同时它也是 `CoroutineContext` 的子接口，因此也可以作为集合出现。

```kotlin
public interface CoroutineContext {
    public operator fun <E : Element> get(key: Key<E>): E?
    public interface Key<E : Element>
    public interface Element : CoroutineContext {
        public val key: Key<*>
        public override operator fun <E : Element> get(key: Key<E>): E? =
            if (this.key == key) this as E else null

    }
}
```

launch函数会返回一个Job。

```kotlin
public interface Job : CoroutineContext.Element {
    /**
     * Key for [Job] instance in the coroutine context.
     */
    public companion object Key : CoroutineContext.Key<Job> 
    ...
 }   
```

我们如果想要找到某一个特别的上下文实现，就需要用对应的 `Key` 来查找，例如：

这里的 `Job` 实际上是对它的 `companion object Key` 的引用

```kotlin
lifecycleScope.launch(CoroutineName("线程名-1")) {
   //获取当前协程的job
		println(coroutineContext[Job])	//等价于coroutineContext[Job.Key]
  	println(job?.isActive)
  
	  //获取coroutineName
	  println(coroutineContext[CoroutineName.Key])
}
```

### 2.拦截器

拦截协程的方法也很简单，因为协程的本质就是回调 + “黑魔法”，而这个回调就是被拦截的 `Continuation` 。

```kotlin
@SinceKotlin("1.3")
public interface ContinuationInterceptor : CoroutineContext.Element {
		companion object Key : CoroutineContext.Key<ContinuationInterceptor>
		public fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
}
```

自己定义一个拦截器:

```kotlin
class MyContinuationInterceptor : ContinuationInterceptor {
    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor.Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        log("MyContinuationInterceptor interceptContinuation")
        return MyContinuation(continuation)
    }
}
fun test(){
  GlobalScope.launch(context = MyContinuationInterceptor())
}
```

所有协程启动的时候，都会有一次 `Continuation.resumeWith` 的操作，这一次操作对于调度器来说就是一次调度的机会。

如果我们在拦截器当中自己处理了线程切换，那么就实现了自己的一个简单的调度器。

### 3.调度器

```kotlin
public abstract class CoroutineDispatcher :
    AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
      public abstract fun dispatch(context: CoroutineContext, block: Runnable)
      public final override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = DispatchedContinuation(this, continuation)
}
```

它本身是协程上下文的子类，同时实现了拦截器的接口， `dispatch` 方法会在拦截器的方法 `interceptContinuation` 中调用，进而实现协程的调度。所以如果我们想要实现自己的调度器，继承这个类就可以了，不过通常我们都用现成的，它们定义在 `Dispatchers` 当中：

```kotlin
public actual val Default: CoroutineDispatcher = createDefaultDispatcher()
public actual val Main: MainCoroutineDispatcher get() = MainDispatcherLoader.dispatcher
public val IO: CoroutineDispatcher = DefaultScheduler.IO
```

## 2.常用挂起函数(suspendCancellableCoroutine、withContext、coroutineScope、supervisorScope)

### 1.launch,async

协程内部使用launch或者async函数会启动一个新的协程，并不是挂起函数，所以后面的代码还是会继续执行，"end"会在"2"之前打印出来, 除非使用join或者await

```kotlin
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

```kotlin
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

```kotlin
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

### 4.coroutineScope和supervisorScope

为了做到结构化并发并避免泄漏的情况发生想要创建多个协程，可以在 suspend function 中使用名为 coroutineScope 或 supervisorScope 这样的构造器来启动多个协程，可以安全地从 suspend 函数中启动协程。

```kotlin
/*
[main]->coroutineScope end
[DefaultDispatcher-worker-2]->launch end
[main]->lifecycleScope end
 */
btn2.setOnClickListener {
    lifecycleScope.launch {
      	//启动一个新协程,但是coroutineScope是挂起函数,会挂起当前线程
        coroutineScope {
            //启动一个新协程,不是挂起函数,后面会继续执行
            launch(Dispatchers.Default) {
                delay(2000)
                log("launch end")
            }
            log("coroutineScope end")
        }

        log("lifecycleScope end")
    }
}
```

### 5.使用区别

#### 1.相同点

suspendCancellableCoroutine、withContext、coroutineScope、supervisorScope的相同点是，他们都是挂起函数，都可以有返回值。

#### 2.不同点

`withContext`可以用来切换线程，但是不能封装回调api，而`suspendCancellableCoroutine`是用来封装回调api，使用`resume` 或者 `resumeWithException` 来返回结果或者抛出异常，只是不能切换线程，但是可以在内部启动线程来切换线程，还有一个重要区别是传给它的block参数并不是一个挂起函数，只是个普通函数（适合用来封装回调api），而传递给`withContext`和`coroutineScope`的block参数都是挂起函数，所以`suspendCancellableCoroutine`的block内部不能使用`delay`这类挂起函数，而`withContext`和`coroutineScope`可以使用delay等挂起函数。`suspendCancellableCoroutine`还具有一个`invokeOnCancellation`来监听协程是否取消。

`coroutineScope`或`supervisorScope`可以创建协程但是它们和launch或者async的区别是，前者是挂起函数，必须在协程中调用，后者只是普通函数，可以在任何地方启动协程，然后挂起函数会让外层函数等待执行完成。

`withContext`和`coroutineScope`都是挂起函数，withContext(IO)可以直接切换线程，而coroutineScope不能传递一个调度器，只能在内部重新launch一个协程才可以切换线程。

##### 使用withContext

```kotlin
lifecycleScope.launch {
    //这里适合用withContext替代coroutineScope就可以少一层嵌套
    withContext(IO) {
        //...耗时操作
    }
}
```

##### 使用CoroutineScope

```kotlin
lifecycleScope.launch {
    //启动一个协程,coroutineScope是一个挂起函数, end将会最后执行
    coroutineScope {
      	//如果内部代码会自己启动一个新线程的话，使用coroutineScope比withContext更好,不然会有两次线程切换
     		//或者其他启动线程方式
        thread{
          //...耗时操作
        }
    }
}
```

##### 使用CoroutineScope

```kotlin
lifecycleScope.launch {
    coroutineScope {
        launch(IO){
          //启动一个协程,外层coroutineScope是一个挂起函数, end将会最后执行
          //...耗时操作
        }
    }
  	println("end")
}
```

##### 使用suspendCancellableCoroutine或者suspendCoroutine封装回调

```kotlin
lifecycleScope.launch {
    suspendCancellableCoroutine {
        getUser{
          	//suspendCancellableCoroutine比suspendCoroutine多一个invokeOnCancellation方法
          	it.invokeOnCancellation{
              log("invokeOnCancellation")
            }
          	//getUser会启动一个线程,比如okhttp的enquee
          	try{
              ...
              it.resume(result)
            }catch(e: Exception){
              it.resumeWithException(e)
            }
        }
    }
  	println("end")
}
```



#### 3.使用场景

在非挂起函数需要封装成协程来使用的时候，比如okhttp等网络请求，view的事件回调等可以使用suspendCancellableCoroutine，

withContext和coroutineScope非常类似，在协程中需要切换线程的时候使用withContext， 不切换线程的时候使用coroutineScope

3个方法的定义如下

```kotlin
public suspend inline fun <T> suspendCancellableCoroutine(
    crossinline block: (CancellableContinuation<T>) -> Unit
): T
```

```kotlin
public suspend fun <T> withContext(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T
```

```kotlin
public suspend fun <R> coroutineScope(
		block: suspend CoroutineScope.() -> R
): R
```

#### 4.具体用例

##### 1.等待 View 布局完成后，获取布局大小

封装了一个等待 View 传递下一次布局事件的任务 (比如说，我们改变了一个 TextView 中的内容，需要等待布局事件完成后才能获取该控件的新尺寸):

```kotlin
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

```kotlin
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



## 5.知识点

1.所有协程启动的时候，都会有一次 `Continuation.resumeWith` 的操作，这一次操作对于调度器来说就是一次调度的机会，我们的协程有机会调度到其他线程的关键之处就在于此。

 2.`delay` 是挂起点，`delay`操作后可能会切换线程，在 JVM 上 `delay` 实际上是在一个 `ScheduledExcecutor` 里面添加了一个延时任务，因此会发生线程切换。

