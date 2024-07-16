# 协程知识点总结

## 1.CoroutineContext(协程上下文、拦截器、调度器)

调度器和拦截器本质上就是一个协程上下文的实现。

### 1.1 上下文

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
GlobalScope.launch(CoroutineName("线程名-1") + Job() + Dispatchers.Default) {
   //获取当前协程的job
		println(coroutineContext[Job])	//等价于coroutineContext[Job.Key]
  	println(job?.isActive)
  
	  //获取coroutineName
	  println(coroutineContext[CoroutineName.Key])
}
```

### 1.2 拦截器

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

### 1.3 调度器

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

## 2. 常用挂起函数(suspendCancellableCoroutine、withContext、coroutineScope、supervisorScope)

### 2.1 launch,async

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

### 2.2 withContext

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



### 2.3 suspendCoroutine和suspendCancellableCoroutine

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

### 2.4 coroutineScope和supervisorScope

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

### 2.5 使用区别

#### 2.5.1 相同点

suspendCancellableCoroutine、withContext、coroutineScope、supervisorScope的相同点是，他们都是挂起函数，都可以有返回值。

#### 2.5.2 不同点

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



#### 2.5.3 使用场景

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

#### 2.5.4 具体用例

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

#### 2.5.5 CallbackFlow(2024-05-13 21:36:28新增)

Callback Flow 是为了与基于回调的 API 一起工作而设计的 Kotlin 的 Flow API 的扩展。它允许你将基于回调的数据转换为一个 Flow，该 Flow 表示异步数据流，并带有一个基于回调的 API。

可以使用它来创建一个 Flow，该 Flow 可以随着回调的触发而接收多个值。这个 Flow 会保持活动，直到它被明确地取消或完成。

在内部，callbackFlow 使用了一个通道，这在概念上非常类似于阻塞队列。awaitClose 在底层使用了 suspendCancellableCoroutine。

处理多次发射的回调：callbackFlow 对于处理多次发射的回调特别有用，这些异步操作会随着时间的推移发射多个结果。例如，你可能在处理 Android 中的位置更新时使用它。

```
val locationListener = object : LocationListener {
    override fun onLocationUpdate(location: Location) {
           
    }
}

LocationManager.registerForLocation(locationListener)
LocationManager.unregisterForLocation(locationListener)

fun getLocationUpdates(): Flow<Location> {
    return callbackFlow {
        val locationListener = object : LocationListener {
            override fun onLocationUpdate(location: Location) {
                trySend(location)
            }
        }
        LocationManager.registerForLocation(locationListener)
        //协程被关闭的时候会调用。
        awaitClose {
            LocationManager.unregisterForLocation(locationListener)
        }
    }
}

launch {
    getLocationFlow()
    .collect { location ->
           
    }
}
```

##### callbackFlow优点：

-  **结构化的异步代码**：callbackFlow 允许你将基于回调的 API 转换为结构化的 Flow，使你的代码更可读和可维护。它避免了回调地狱，并简化了错误处理。
-  **取消处理**：当不再需要 Flow 时，它会自动处理取消，确保资源被正确释放。这减少了你的 Android 应用中资源泄露的风险。
-  **并发数据处理**：Coroutines 和 Flows 与 callbackFlow 无缝工作，使你能够并发处理数据，并处理多个异步操作，并行操作，而无需嵌套回调的复杂性。
-  **与其他协程特性的集成**：callbackFlow 很好地与其他协程特性集成，如挂起函数，async 和 await，允许你构建复杂的异步工作流。

##### callbackFlow缺点：

-  **复杂性**：CallbackFlow 可能会给你的代码引入复杂性，特别是当你必须管理多个回调和流发射时。这可能使代码更难阅读和维护。
-  **调试**：使用 callbackFlow 调试异步代码可能更具挑战性，因为它可能不会提供像结构化并发的 async/await 那样清晰的堆栈跟踪。

##### suspendCancellableCoroutine 的优点：

-  **细粒度控制**：可以完全控制协程的生命周期，包括取消。这允许你在协程被取消时清理资源并执行自定义操作。
-  **与基于回调的 API 的集成**：对于需要处理回调但想要提供更结构化和协程友好接口的现有异步基于回调的 API，它非常有用。
-  **自定义错误处理**：可以定义如何在协程内部处理错误，使得在协程内部管理和传播异常变得更加容易。

##### suspendCancellableCoroutine的缺点：

-  **复杂性**：使用带有回调的 suspendCancellableCoroutine 可能比像 async、launch 或 withContext 这样的更高级别的抽象更复杂和容易出错。你需要手动管理协程的生命周期、取消和错误处理。
-  **不适合大多数用例**：对于大多数用例，使用像 async、launch 或 callbackFlow 这样的更高级别的构造更直接，也更不容易出错。suspendCancellableCoroutine 通常保留用于低级操作或没有更好替代方案的情况。
-  **需要小心处理**：在使用 suspendCancellableCoroutine 时，必须小心确保正确地处理取消和异常，因为不恰当的使用可能导致资源泄漏或意外行为。

## 3.CoroutineScope(协程作用域)

1.创建一个CoroutineScope协程作用域需要传入一个CoroutineContext

比如:

```kotlin
public fun MainScope(): CoroutineScope = ContextScope(SupervisorJob() + Dispatchers.Main)
```

## 4.异常处理

### `coroutineScope`与`supervisorScope`

当一个协程由于一个异常而运行失败时，它会传播这个异常并传递给它的父级。接下来，父级会进行下面几步操作:

- 取消它自己的子级；

- 取消它自己；

- 将异常传播并传递给它的父级。

异常会到达层级的根部，而且当前 `CoroutineScope` 所启动的所有协程都会被取消。

使用 `SupervisorJob` 时，一个子协程运行失败不会影响到其他子协程。`SupervisorJob` 不会取消它和它自己的子级，也不会传播异常并传递给它的父级，它会让子协程自己处理异常。

- coroutineScope 是继承外部 Job 的上下文创建作用域，在其内部的取消操作是双向传播的，子协程未捕获的异常也会向上传递给父协程。它更适合一系列对等的协程并发的完成一项工作，任何一个子协程异常退出，那么整体都将退出，这也是协程内部再启动子协程的默认作用域。
- supervisorScope 同样继承外部作用域的上下文，但其内部的取消操作是单向传播的，父协程向子协程传播，反过来则不然，这意味着子协程出了异常并不会影响父协程以及其他兄弟协程。它更适合一些独立不相干的任务，任何一个任务出问题，并不会影响其他任务的工作，例如 UI，我点击一个按钮出了异常，其实并不会影响手机状态栏的刷新。**需要注意的是，supervisorScope 内部启动的子协程内部再启动子协程，如无明确指出，则遵守默认作用域规则，也即 supervisorScope 只作用域其直接子协程。**

**未被捕获的异常一定会被抛出，无论您使用的是哪种 Job**

使用 `coroutineScope` 和 `supervisorScope` 也有Job和SupervisorJob相同的效果。它们会创建一个子作用域 (使用一个 Job 或 SupervisorJob 作为父级)

**如果想要在出现错误时不会退出父级和其他平级的协程，那就使用 SupervisorJob 或 supervisorScope。**

```kotlin
val jobScope = CoroutineScope(Job() + CoroutineName("my-job-scope"))
val supervisorScope = CoroutineScope(SupervisorJob() + CoroutineName("my-job-scope"))

/*
		app crash
    Child 1 失败了，无论是 scope 还是 Child 2 都会被取消。
    输出:
    1
    KotlinNullPointerException
 */
fun test1() {
    jobScope.launch {
        //child 1
        println("1")
        throw KotlinNullPointerException()
    }

    jobScope.launch {
        //child 2
        delay(1000)
        println("2")
    }
}

		/*
				app crash
				当前代码如果放在android中执行,2不会被输出, 因为异常没有被捕获,导致app crash.
        使用supervisorScope时, Child 1 失败了，scope 和 Child 2 不会被取消。
    
        输出:
        1
        KotlinNullPointerException
        app crash
     */
    fun test2(){
        supervisorScope.launch {
            //child 1
            println(1)
            throw KotlinNullPointerException()
        }

        supervisorScope.launch {
            //child 2
            delay(1000)
            println(2)
        }
    }

				/*
				    app crash
            android中app直接crash导致2不会输出, 在jvm的测试中2是可以输出的
            输出:
            1
            KotlinNullPointerException
         */
        btn3.setOnClickListener {
            jobScope.launch {
               //这里加try-catch是捕获不到异常的, 除非把supervisorScope换成coroutineScope, 异常才会被正常抛出, 不然子协程会自己处理异常
               //如果使用 coroutineScope 代替 supervisorScope ，错误就会被传播，作用域最终也会被取消
                supervisorScope {
                    launch {
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
                          	//end-2不会被输出, 因为使用的是coroutineScope
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
				//直接对launch进行try-catch是捕获不到的
        btn5.setOnClickListener {
            jobScope.launch {
                try {
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
```

只有使用 supervisorScope 或 CoroutineScope(SupervisorJob()) 创建 SupervisorJob 时，它才会像前面描述的一样工作。将 SupervisorJob 作为参数传入一个协程的 Builder 不能带来想要的效果：

```kotlin
/*
  Child 1 的父级 Job 就只是 Job 类型! 虽然乍一看确实会让人以为是 SupervisorJob，但是因为新的协程被创建时，会生成新的 Job 实例替代 SupervisorJob，所以这里并不是。本例中的 SupervisorJob 是协程的父级通过 scope.launch 创建的，所以真相是，SupervisorJob 在这段代码中完全没用！
  
  输出:
  app crash
  1
  KotlinNullPointerException
     */
fun test4(){
  jobScope.launch(SupervisorJob()) {
    launch {
      // child1
      println("1")
      throw KotlinNullPointerException()
    }

    launch {
      // child2
      delay(1000)
      println("2")
    }
  } 
}


```



```kotlin
lifecycleScope.launch {
    try {
        //这里用supervisorScope或者CoroutineScope均可以捕获到异常
        //因为withContext并不会开启一个新的协程,所以它被取消后均可以在外层catch到
        //受影响的只是里面的launch启动的协程
        supervisorScope {
            println(coroutineContext[Job.Key])

            launch {
                try {
                    //这里就算是使用了supervisorScope, 也会被下面withContext的异常所取消,
                    //所以这里的supervisorScope根本没有用!!!
                    //因为withContext的异常会取消掉supervisorScope, supervisorScope又是这里launch的父协程
                    //父协程取消了,子协程肯定会被取消, 想不被取消看下面例子
                    supervisorScope {
                        println(coroutineContext[Job.Key])
                        delay(3000)
                        println("delay-end")
                    }
                } catch (e: Exception) {
										//会打印出取消异常信息
                    // JobCancellationException: Parent job is Cancelling; 										job=ScopeCoroutine{Cancelling}@9d362ed
                    println(e)
                }
            }


						//这里用withContext和不用,结果都是一样, 因为withContext不会启动一个新协程
            //withContext(Dispatchers.Default) {
                println(coroutineContext[Job.Key])
                delay(1000)
                throw KotlinNullPointerException()
            //}
        }
    } catch (e: Exception) {
        println(e)
      	//KotlinNullPointerException
    }
}
```

```kotlin
lifecycleScope.launch {
    try {
        //这里用supervisorScope或者CoroutineScope均可以捕获到异常
        //因为withContext并不会开启一个新的协程,所以它被取消后均可以在外层catch到
        //受影响的只是里面的launch启动的协程
        supervisorScope {
            println(coroutineContext[Job.Key])

            //方法1:launch想要不被下面的异常所取消, 可以在这里使用Job或者SupervisorJob
            //因为这样就是单独的一个作用域,不受父协程控制
            launch(Job()) {
                try {
                    println(coroutineContext[Job.Key])
                    delay(3000)
                    //3000ms后 delay-end会顺利输出
                    println("delay-end")
                } catch (e: Exception) {
                    //这里不会输出异常
                    println(e)
                }
            }

            println("withContext")
						//这里用withContext和不用,结果都是一样, 因为withContext不会启动一个新协程
            //withContext(Dispatchers.Default) {
                println(coroutineContext[Job.Key])
                delay(1000)
                throw KotlinNullPointerException()
            //}
        }
    } catch (e: Exception) {
        println(e)
      	//KotlinNullPointerException
    }
}
```

### Launch

使用 launch 时，异常会在它发生的第一时间被抛出，这样就可以将抛出异常的代码包裹到 try/catch 中，就像下面这样， 在launch外层进行try-catch是不能捕捉到的，会导致app-crash

正确做法:

```kotlin

scope.launch{
	try {
		throw Exception()
	} catch (e) {
    // 处理异常
	}
}
```

错误做法(这样会捕捉不到导致app-crash)：

```kotlin
//app crash
//捕捉不到异常
//也可以在launch外面套一个coroutineScope, 就可以顺利的捕获到异常
jobScope.launch {
    try {
        launch {
            println("child-1")
            delay(1000)
            throw KotlinNullPointerException("exception")
        }
    } catch (e: Exception) {
        println(e.message)
    }
}
```

使用coroutineScope也可以成功的捕获到异常:

```kotlin
//app不会crash
//能捕捉到异常
//输出:
//child-1
//KotlinNullPointerException
jobScope.launch {
    try {
      //不能用supervisorScope, supervisorScope会让子协程自己处理异常就会导致外层catch不到
      coroutineScope {
          launch {
              println("child-1")
              delay(1000)
              throw KotlinNullPointerException("exception")
          }
      } catch (e: Exception) {
          println(e)
      }
    }
}
```



### Async

1. 外面套了supervisorScope, async不会第一时间抛出异常而是等到await的时候
2. 外面套了coroutineScope, async还是会第一时间抛出异常
3. async使用SupervisorJob()来启动, async不会第一时间抛出异常而是等到await的时候
4. async作为根协程来启动, async不会第一时间抛出异常而是等到await的时候

当 async 被用作根协程 (CoroutineScope 实例或 supervisorScope 的直接子协程) 时**不会自动抛出异常，而是在调用 .await() 时才会抛出异常。**为了捕获其中抛出的异常，用 try/catch 包裹调用 .await() 

```kotlin
btn6.setOnClickListener {
    //jobScope.async不是挂起函数可以在这里直接启动, 但是下面的await是挂起函数，必须在协程体重调用
    //所以这里要用jobScope.launch来启动一个协程体
  	//app不会crash
  	//输出:
  	//1
  	//KotlinNullPointerException
    jobScope.launch {
      	//注意这里是使用jobScope.async,而不是直接async
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
```

如果不进行await()的话, 异常永远都不会被抛出：

```kotlin
				/**
         * app不会crash
         * 如果不进行await()的话, 异常永远都不会被抛出
         * 输出:
         * 1
         */
btn7.setOnClickListener {
    val deferred = jobScope.async {
        println("1")
        delay(500)
        if (true) {
            throw KotlinNullPointerException()
        }
        "result"
    }
}
```

todo:这里不是太理解, 后面补充

由于 jobScope 的直接子协程是 launch，**async (CoroutineContext中Job继承父级) 会自动传播异常到它的父级 (launch)**，这会让异常被立即抛出(**注意这里异常会被捕获, 但是app还是会crash**)：

```kotlin
//todo:这里不是太理解, 后面补充(2024-05-13 22:07:04已经理解,会crash的原因是使用了
//async会导致异常被立即抛出所以外部的scope也出现异常)
//app crash
//注意这里异常会被捕获, 但是app还是会crash
//输出:
//1
//KotlinNullPointerException
btn8.setOnClickListener {
    jobScope.launch {
        try {
          	//todo:这里不是太理解, 后面补充
          	//这里的async的上下文继承了父级, job也继承了父级job
          	//这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
            val deferred = async {
                println("1")
                delay(500)
                throw KotlinNullPointerException()
            }

            //deferred.await()
        } catch (e: Exception) {
            println(e)
        }
    }
}
```

```kotlin
//todo:这里不是太理解, 后面补充
//app不会crash
//由于 scope 的直接子协程是 launch，如果 async 中产生了一个异常，这个异常将会被立即抛出。
//原因是 async (包含一个 Job 在它的 CoroutineContext 中) 会自动传播异常到它的父级 (launch)，
//这会让异常被立即抛出。
//async非根协程启动,会立即抛出异常所以外层可以try到, 然后外层又会继续抛出,所以最外层又能try到第二次异常
//输出:
//1
//1------>KotlinNullPointerException
//2------>KotlinNullPointerException
btn9.setOnClickListener {
    jobScope.launch {
        try {
            coroutineScope {
                try {
                    //这里如果改成jobScope.async{}, 异常就会被正确捕获
                    //async非根协程启动,会立即抛出异常所以外层可以try到, 然后外层又会继续抛出,所以最外层又能try到第二次异常
                    val deferred = async {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()
                    }

                    //这里不执行await也会导致抛出异常,且不被捕获
                    deferred.await()
                } catch (e: Exception) {
                  //可以捕获,但是会传递异常到父协程
                    println("1------>$e")
                }
            }
        } catch (e: Exception) {
            println("2------>$e")
        }
    }
}


//app不会crash
//输出
// 1
// 1------>KotlinNullPointerException
btn10.setOnClickListener {
  jobScope.launch {
    try {
      coroutineScope {
        //async非根协程启动,会立即抛出异常所以外层可以try到
        //这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
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

//app不会crash
//输出
// 1
// 1------>KotlinNullPointerException
btn11.setOnClickListener {
  jobScope.launch {
    try {
      supervisorScope {
        //这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
        //async非根协程启动,会立即抛出异常所以外层可以try到
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
```

### 其他协程所创建的协程中产生的异常总是会被传播，无论协程的 Builder 是什么。如下：

由于 scope 的直接子协程是 launch，如果 async 中产生了一个异常，这个异常将就会被立即抛出。原因是 async (包含一个 Job 在它的 CoroutineContext 中) 会自动传播异常到它的父级 (launch)，这会让异常被立即抛出。

**在 coroutineScope builder 或在其他协程创建的协程中抛出的异常不会被 try/catch 捕获！**

```kotlin
// 如果 async 抛出异常，launch 就会立即抛出异常，而不会调用 .await()
//输出:
//1
//app crash
btn7.setOnClickListener {
    jobScope.launch {
        val deferred = async {
            println("1")
            delay(500)
            throw KotlinNullPointerException()
        }
    }
}
```

## 5.知识点

1.所有协程启动的时候，都会有一次 `Continuation.resumeWith` 的操作，这一次操作对于调度器来说就是一次调度的机会，我们的协程有机会调度到其他线程的关键之处就在于此。

 2.`delay` 是挂起点，`delay`操作后可能会切换线程，在 JVM 上 `delay` 实际上是在一个 `ScheduledExcecutor` 里面添加了一个延时任务，因此会发生线程切换。



## 6.协程的去抖动、节流、重试选项

### debounce 去抖动

![图片](https://mmbiz.qpic.cn/sz_mmbiz_jpg/7G6wAxO5rWDNJgFR0rcFksn4NiaUMDIRRB5icsAHqh9l7YyxLTakjgibNv1jHs2hia1AZian4oqxQS02e1MN3mMHT7A/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

对函数进行去抖动意味着所有调用都将被忽略，直到它们停止一段时间。只有这样该函数才会被调用。例如，如果我们将计时器设置为 2 秒，并且该函数以 1 秒的间隔调用 10 次，则实际调用将在最后一次（第十次）调用该函数后仅 2 秒发生。

该方法的本质是在接收到最后一个流值后会等待一定的时间间隔。如果在此期间收到新值，则间隔将重新开始。如果在等待间隔期间没有收到新值，则最后一个值将输出到最终线程。

例如，如果使用一个线程在每次用户输入字符时更新电话查找，那么使用 debounce 方法，电话将不会随着每次更改而不断出现和消失。相反，在用户完成键入后，更新查找会延迟一定时间。

假设我们的任务是处理用户在搜索字段中的输入，但只有在用户完成输入数据后才需要将数据发送到服务器。在这种情况下，可以使用 debounce 方法，仅在用户输入完成后才处理用户的输入。

```
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
fun main() = runBlocking<Unit> {
 val searchQuery = MutableStateFlow("")
 
 // set a delay of 500 ms before each retrieved value
 val result = searchQuery.debounce(500)
 
 // subscribing to receive values from the result
 result.collect { 
   // send a request to the server only if the result of the search string is not empty
   if (it.isNotBlank()) {
     println("Request to server: $it")
   } else {
     println("The search string is empty")
   }
 }
 
 // simulate user input in the search field
 launch {
 delay(0)
 searchQuery.value = "a"
 delay(100)
 searchQuery.value = "ap"
 delay(100)
 searchQuery.value = "app"
 delay(1000)
 searchQuery.value = "apple"
 delay(1000)
 searchQuery.value = ""
 }
}
```

在此示例中，搜索字段由 MutableStateFlow 表示。去抖方法在检索每个值之前设置 0.5 秒的延迟。在最后一个线程中，仅当搜索字符串不为空时才会向服务器发出请求。

执行此代码的结果将打印到控制台：

Request to server: app
Request to server: apple
The search string is empty

这意味着仅在用户在搜索字段中输入完数据后才向服务器发送请求。



### Throttling 节流

![图片](https://mmbiz.qpic.cn/sz_mmbiz_jpg/7G6wAxO5rWDNJgFR0rcFksn4NiaUMDIRRvy7mxbI6JFWQXRh19Lt3EH8Jiala1G9uTEXFmFZK46kz6aYkee0yLDQ/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

函数限制是指在指定时间段内调用该函数不超过一次（例如每 10 秒一次）。换句话说，如果某个函数最近已经被调用过，trattting 会阻止该函数被调用。 Trattting 还确保该函数定期执行。

throttle 方法与 debounce 类似，因为它也用于控制线程内发送项目的频率。这些方法之间的区别在于它们如何处理接收到的值。与防抖不同的是，防抖会忽略除最后一个值之外的所有值，而节流阀会记住最后一个值，并在设置的时间间隔到期时重置计时器。如果没有出现新值，则将最后存储的值发送到输出流。

因此，debounce 会忽略除最后一个值之外的所有值，并仅在经过一定时间后发送最后一个值，而不会收到新值。 Throttle 会记住最后一个值，并在每次一定时间间隔后发送它，无论该时间段内收到的值数量如何。

两种方法都适用于不同的场景，方法的选择取决于所需的数据处理逻辑。

### Retry 重试

使用 Kotlin 协程，如果上次操作失败，可以在一段时间后轻松重试操作。为此，可以使用 retryWhen 函数，该函数允许确定应重复操作的频率和次数。

retryWhen 函数应与标准 Kotlin 库“kotlinx.coroutines”中的 catch 语句结合使用。 catch 语句用于捕获操作期间可能发生的任何异常。

但是，retryWhen 方法是作为 Flow 的扩展来实现的。要允许操作独立于流程执行一次，请考虑其自己的实现：

```
suspend fun loadResource(url: String): Resource {
 // loadResource by url
}

suspend fun getResourceWithRetry(url: String, retries: Int, intervalMillis: Long): Resource {
 return try {
   loadResource(url)
 } catch (e: Exception) {
   if (retries > 0) {
     delay(intervalMillis) // a delay for a certain period of time
     getResourceWithRetry(url, retries - 1, intervalMillis) // repeat the operation after a certain period of time
   } else {
     throw e // throw an exception if retries are expired
   }
 }
}

// example of use
CoroutineScope(Dispatchers.IO).launch {
 val resource = getResourceWithRetry("http://example.com/resource", 3, 1000)
 // use of the loaded resource
}
```

这里我们定义了一个 getResourceWithRetry 函数，它调用 loadResource 操作来加载给定 URL 处的资源。如果操作不成功，将使用延迟函数递归调用函数。

尝试重复操作的次数由retries参数决定，重试之间的时间间隔由intervalMillis参数决定。

为了处理异常，在 loadResource 函数调用周围使用 catch 语句。如果操作失败，会再次调用 getResourceWithRetry 函数，重试次数减少 1，并延迟 IntervalMillis 参数定义的时间间隔。

这样，如果上次失败，可以使用 retryWhen 函数和 catch 运算符轻松地在一段时间后重新运行该操作。

所提出的算法有两个缺点：它执行一个固定操作（访问网络中的特定地址）并且以恒定的间隔执行。第一个问题可以通过使用模板方法来解决，该方法允许用必要的操作来代替可能失败的代码调用。第二个问题可以通过稍微复杂地计算尝试之间的间隔来解决（这尤其重要，例如，在访问远程服务器时 - 如果许多客户端以相同的间隔重复尝试，则存在增加服务器上的负载达到临界水平）。

因此，我们有一个更灵活的选择：

```
suspend fun <T> getResourceWithRetry(
 retries: Int = 5, // 5 retries
 initialDelay: Long = 100L, // 0.1 second
 maxDelay: Long = 128000L, // 128 seconds
 factor: Double = 2.0,
 block: suspend () -> T): T
{
 return try {
   loadResource(url)
 } catch (e: Exception) {
   if (retries > 0) {
     val currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
     delay(currentDelay) // delay for a certain period of time
     getResourceWithRetry(retries - 1, currentDelay, maxDelay, factor, block) // repeat the operation after some time
   } else {
     throw e // throw an exception if retries are over
   }
}
```



## 7.repeatOnLifecycle和launchWhenResumed的区别

1. launchWhenResumed只是挂起和恢复, 当生命周期小于Resumed的时候比如
   stoped(实际上不是小于,需要分析源码,Resumed对应的取消事件是ON_PAUSE, 当
   接收到ON_PAUSE的时候取消该协程)的时候挂起该线程,然后当回到Resumed的时候重
   新执行该协程,类似线程中的wait和notify
2. repeatOnLifecycle会在当前生命周期大于等于STARTED的时候执行里面的方法,
   然后小于该生命周期后cancel掉该协程Job

```
// 由于 repeatOnLifecycle 是一个挂起函数，
// 因此要从 lifecycleScope 中创建新的协程
lifecycleScope.launch {
    // 直到 lifecycle 进入 DESTROYED 状态前都将当前协程挂起。
    // repeatOnLifecycle 每当生命周期处于 STARTED 或以后的状态时会在新的协程中
    // 启动执行代码块，并在生命周期进入 STOPPED 时取消协程。
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        // 当生命周期处于 STARTED 时安全地从 locations 中获取数据
        // 当生命周期进入 STOPPED 时停止收集数据
        someLocationProvider.locations.collect {
            // 新的位置！更新地图（信息）
        }
    }
    // 注意：运行到此处时，生命周期已经处于 DESTROYED 状态！
}
```



创建了一个方便的封装函数，名字叫作 launchAndCollectIn:

```Kotlin
Kotlin

复制代码inline fun <T> Flow<T>.launchAndCollectIn(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend CoroutineScope.(T) -> Unit
) = owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(minActiveState) {
            collect {
                action(it)
            }
        }
    }
```

你可以在 UI 代码中这样调用它:

```Kotlin
Kotlin

复制代码class LocationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        someLocationProvider.locations.launchAndCollectIn(this, STARTED) {
            // 新的位置！更新地图（信息）
        }
    }
}
```

这个封装函数，虽然如同例子里那样看起来非常简洁和直接，但也存在同上文的 `LifecycleOwner.addRepeatingJob` API 一样的问题: 它不管调用的作用域，并且在用于其他协程内部时有潜在的危险。进一步说，原来的名字非常容易产生误导: collectIn 不是一个挂起函数！如前文提到的那样，开发者希望名字里带 collect 的函数能够挂起。或许，这个封装函数更好的名字是 Flow.launchAndCollectIn，这样就能避免误用了。





## **8.**Flow.flowWithLifecycle

若只需收集一个数据流，可以使用 `Flow.flowWithLifecycle` 操作符，其内部也是使用 `suspend Lifecycle.repeatOnLifecycle` 函数实现.

`Flow.flowWithLifecycle` 操作符 (您可以参考 [具体实现](https://link.juejin.cn/?target=https%3A%2F%2Fcs.android.com%2Fandroidx%2Fplatform%2Fframeworks%2Fsupport%2F%2B%2Fandroidx-main%3Alifecycle%2Flifecycle-runtime-ktx%2Fsrc%2Fmain%2Fjava%2Fandroidx%2Flifecycle%2FFlowExt.kt%3Bl%3D87)) 是构建在 `repeatOnLifecycle` 之上的，并且仅当生命周期至少处于 `minActiveState` 时才会将来自上游数据流的内容发送出去。

```
lifecycleScope.launch {
            someLocationProvider.locations
                .flowWithLifecycle(lifecycle, STARTED)
                .collect {
                    // 新的位置！更新地图（信息）
                }
        }
```

```
class LocationActivity : AppCompatActivity() {
​    override fun onCreate(savedInstanceState: Bundle?) {
​        super.onCreate(savedInstanceState)
​        locationProvider.locationFlow()
​            .flowWithLifecycle(this, Lifecycle.State.STARTED)
​            .onEach {
​                // 新的位置！更新地图
​            }
​            .launchIn(lifecycleScope) 
​    }
}
```



## 9.在使用 Kotlin 协程时应该注意的事情

### 1.使用 coroutineScope 包装异步调用或使用 SupervisorJob 处理异常

❌ 如果异步块可能抛出异常，请不要依赖于用 try/catch 块包装它:

```
val job: Job = Job()
val scope = CoroutineScope(Dispatchers.Default + job)
// may throw Exception
fun doWork(): Deferred<String> = scope.async { 抛出异常 }   // (1)
fun loadData() = scope.launch {
    try {
        doWork().await()                               // (2)
    } catch (e: Exception) { ... }
}
```

在上面的示例中，doWork 函数启动新的协程 (1)，这可能会引发未处理的异常。如果您尝试使用 try/catch 块 包装 doWork(2) ，它仍然会导致scope启动的其他协程被取消。

发生这种情况是因为**非SupervisorJob的情况下任何子协程的失败都会导致其父协程立即失败。**

✅ 避免崩溃的一种方法是使用 SupervisorJob(1):

**协程的失败或取消不会导致主协程失败，也不会影响其其他子协程。**

```
val job = SupervisorJob()                               // (1)
val scope = CoroutineScope(Dispatchers.Default + job)

// may throw Exception
fun doWork(): Deferred<String> = scope.async { ... }

fun loadData() = scope.launch {
    try {
        doWork().await()
    } catch (e: Exception) { ... }
}
```



❌ 注意：只有当您使用 SupervisorJob 在根协程范围内运行async时，这才有效。因此，下面的代码仍然会使您的应用程序崩溃，因为async是在非根协程的范围内启动的。

```
val job = SupervisorJob()                               
val scope = CoroutineScope(Dispatchers.Default + job)

fun loadData() = scope.launch {
    try {
    		//async作为非根协程被启动时, 会自动传播异常到它的父级，这会让异常被立即抛出(注意这里异常会被捕获, 但是app还是会crash
        async {                                         // (1)
            // may throw Exception 
        }.await()
        //可以捕获,但是会传递异常到父协程
    } catch (e: Exception) { ... }
}
```

✅ 另一种避免崩溃的方法（这是更好的方法）是使用 coroutineScope包装 async。现在，当 async 内部发生异常时，它将取消在此范围内创建的所有其他协程，而不会触及外部范围。 (2)可以在异步块内处理异常。

```
val job = SupervisorJob()                               
val scope = CoroutineScope(Dispatchers.Default + job)

// may throw Exception
suspend fun doWork(): String = coroutineScope {     // (1)
    async { ... }.await()
}

fun loadData() = scope.launch {                       // (2)
    try {
        doWork()
    } catch (e: Exception) { ... }
}
```



### 2.避免使用不必要的 async/await

❌如果您正在使用async，然后立即await，您应该停止这样做。

```
launch {
    val data = async(Dispatchers.Default) { /* code */ }.await()
}
```

✅ 如果你想切换协程上下文并立即挂起父协程，withContext 是更好的方法。

```
launch {
    val data = withContext(Dispatchers.Default) { /* code */ }
}
```

从性能角度来看，这并不是一个大问题（即使异步会创建新的协程来完成工作），但从语义上讲，异步意味着您想要在后台启动多个协程，然后才等待它们。

**async适合多个任务并发操作, 如果只有一个的话适合使用withContext**



### 3.避免取消范围作业

❌ 如果你需要取消协程，首先不要取消作用域作业。

```
class WorkManager {
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Default + job)
    
    fun doWork1() {
        scope.launch { /* do work */ }
    }
    
    fun doWork2() {
        scope.launch { /* do work */ }
    }
    
    fun cancelAllWork() {
        job.cancel()
    }
}

fun main() {
    val workManager = WorkManager()
    
    workManager.doWork1()
    workManager.doWork2()
    workManager.cancelAllWork()
    workManager.doWork1() // (1)
}
```

上述代码的问题在于，当我们取消作业时，我们将其置于已完成状态。在已完成作业范围内启动的协程将不会被执行 (1)。

 ✅ 当你想取消特定作用域的所有协程时，可以使用cancelChildren函数。此外，提供取消个别作业的可能性也是一个很好的做法 (2)。

```
class WorkManager {
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Default + job)
    
    fun doWork1(): Job = scope.launch { /* do work */ } // (2)
    
    fun doWork2(): Job = scope.launch { /* do work */ } // (2)
    
    fun cancelAllWork() {
        scope.coroutineContext.cancelChildren()         // (1)                             
    }
}
fun main() {
    val workManager = WorkManager()
    
    workManager.doWork1()
    workManager.doWork2()
    workManager.cancelAllWork()
    workManager.doWork1()
}
```

### 4.避免使用全局范围

❌ 如果您在 Android 应用程序中到处使用 GlobalScope，您应该停止这样做。

```
GlobalScope.launch {
    // code
}
```

全局范围用于启动顶级协程，这些协程在整个应用程序生命周期内运行并且不会提前取消。

应用程序代码通常应使用应用程序定义的 CoroutineScope，强烈建议不要在 GlobalScope 实例上使用 async 或 launch。

✅ 在 Android 协程中，可以轻松地将范围限定为 Activity、Fragment、View 或 ViewModel 生命周期。

```
class MainActivity : AppCompatActivity(), CoroutineScope {
    
    private val job = SupervisorJob()
    
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
    }
    
    fun loadData() = launch {
        // code
    }
}
```





## 10.SharedFlow  和  StateFlow

### SharedFlow 的构造函数

```Kotlin
Kotlin

复制代码public fun <T> MutableSharedFlow(
    replay: Int = 0,
    extraBufferCapacity: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): MutableSharedFlow<T>
```

主要有三个参数：

- replay: 新订阅者 collect 时，发送 replay 个历史数据给它，默认新订阅者不会获取以前的数据。
- extraBufferCapacity: MutableSharedFlow 缓存的数据个数为 replay + extraBufferCapacity; 缓存一方面用于粘性事件的发送，另一方面也为了处理背压问题，既下游的消费者的消费速度低于上游生产者的生产速度时，数据会被放在缓存中。
- onBufferOverflow: 背压处理策略，缓存区满后怎么处理(挂起或丢弃数据)，默认挂起。注意，当没有订阅者时，只有最近 replay 个数的数据会存入缓存区，不会触发 onBufferOverflow 策略。



### StateFlow 的构造函数：

```Kotlin
Kotlin

复制代码public fun <T> MutableStateFlow(value: T): MutableStateFlow<T>
```

构造函数只需传入一个初始值，它本质上是一个 replay = 1 且没有缓冲区的 SharedFlow, 因此在第一次订阅时会先获取到初始值。



### StateFlow 与 LiveData区别

共同点：

- 都提供了 `可读写` 和 `只读` 两个版本。
- 值唯一，会把最新值重现给订阅者，即粘性。
- 可被多个订阅者订阅(共享数据流)。
- 子线程中多次发送数据可能会丢失数据。

不同点：

- StateFlow 必须传入初始值，保证值的空安全，永远有值。
- StateFlow 可以防抖，即多次传入相同值，不会有响应(Setting a value that is [equal][Any.equals] to the previous one does nothing) 。
- 当 View 进入 STOPPED 状态时，`LiveData.observe()` 会停止接收数据，而从 StateFlow 或其他 Flow collect 数据的操作并不会自动停止。如需实现相同的行为，需要从 `Lifecycle.repeatOnLifecycle` 块 collect 数据流。

 `StateFlow` 与 `LiveData` 有一些相同点：

- 提供「可读可写」和「仅可读」两个版本（`StateFlow`，`MutableStateFlow`）
- 它的值是唯一的
- 它允许被多个观察者共用 （因此是共享的数据流）
- 它永远只会把最新的值重现给订阅者，这与活跃观察者的数量是无关的
- 支持 `DataBinding`

它们也有些不同点：

- StateFlow必须配置初始值

- StateFlow value 空安全

- StateFlow默认是防抖的，在更新数据时，会判断当前值与新值是否相同，如果相同则不更新数据。

  

`MutableStateFlow` 构造方法强制赋值一个非空的数据，而且 value 也是非空的。这意味着 `StateFlow` 永远有值

> StateFlow 的 `emit()` 和 `tryEmit()` 方法内部实现是一样的，都是调用 `setValue()`



### StateFlow与ShareFlow主要区别:

1. StateFlow 默认replay为1

2. SharedFlow 没有起始值

3. 状态（State）用 StateFlow ；事件（Event）用 SharedFlow  

4. `StateFlow`始终是有值的且值是唯一的，创建时需要赋予初始值

5. `StateFlow`永远只会把最新的值重现给订阅者，与活跃观察者数量无关

6. 如果`StateFlow`重复赋予同一个值，只会回调一次

7. `MutableSharedFlow` 发射值需要调用 `emit()/tryEmit()` 方法，**没有** `setValue()` 方法

8. SharedFlow 可以传入一个 replay 参数，它表示可以对新订阅者重新发送 replay 个历史数据，默认值为 0, 即非粘性。

9. StateFlow 可以看成是一个 replay = 1 且没有缓冲区的 SharedFlow 。

10. SharedFlow 在子线程中多次 emit() 不会丢失数据, 可以保留历史数据

11. MutableSharedFlow 发射值需要调用 emit()/tryEmit() 方法，没有 setValue() 方法

12. 与 MutableSharedFlow 不同，MutableSharedFlow 构造器中是不能传入默认值的，这意味着 MutableSharedFlow 没有默认值。

13. StateFlow 与 SharedFlow 还有一个区别是StateFlow只保留最新值，即新的订阅者只会获得最新的和之后的数据,而 `SharedFlow` 根据配置可以保留历史数据，新的订阅者可以获取之前发射过的一系列数据。

14. StateFlow 默认是防抖的，在更新数据时，会判断当前值与新值是否相同，如果相同则不更新数据。

15. SharedFlow和StateFlow 执行tryEmit或者emit都不会等到collect收集后再继续执行(sharedFlow设置为

    BufferOverflow.DROP_OLDEST不会挂起, 设置为BufferOverflow.suspend会挂起),而是直接继续执行后面的操作

16. MutableSharedFlow(replay = 0,extraBufferCapacity = 0,onBufferOverflow = BufferOverflow.SUSPEND)
    参数含义：
    `replay`：新订阅者订阅时，重新发送多少个之前已发出的值给新订阅者（类似粘性数据）；
    `extraBufferCapacity`：除了 replay 外，缓存的值数量，当缓存空间还有值时，emit 不会 suspend（emit 过快，collect 过慢，emit 的数据将被缓存起来）；
    `onBufferOverflow`：指定缓存区中已存满要发送的数据项时的处理策略（缓存区大小由 `replay` 和 `extraBufferCapacity` 共同决定）。默认值为 `BufferOverflow.SUSPEND`，还可以是 `BufferOverflow.DROP_LATEST` 或 BufferOverflow.DROP_OLDEST。

17. `BufferOverflow 枚举类`：背压处理策略，缓存区满后怎么处理(挂起或丢弃数据)，默认挂起。 `SUSPEND` 表示发送或发送值的上游在缓存区已满时挂起，枚举值`DROP_OLDEST` 表示溢出时删除缓存区中最旧的值，将新值添加到缓存区，不要挂起，枚举值 `DROP_LATEST` 表示在缓存区溢出时删除当前添加到缓存区的最新值（以便缓存区内容保持不变），不要挂起；

    

### SharedFlow业务场景

SharedFlow 实现的 EventBus 简单例子：

```
object EventBus {
    private val events = ConcurrentHashMap<String, MutableSharedFlow<Event>>()

    private fun getOrPutEventFlow(eventName: String): MutableSharedFlow<Event> {
        return events[eventName] ?: MutableSharedFlow<Event>().also { events[eventName] = it }
    }

    fun getEventFlow(event: Class<Event>): SharedFlow<Event> {
        return getOrPutEventFlow(event.simpleName).asSharedFlow()
    }

    suspend fun produceEvent(event: Event) {
        val eventName = event::class.java.simpleName
        getOrPutEventFlow(eventName).emit(event)
    }

    fun postEvent(event: Event, delay: Long = 0, scope: CoroutineScope = MainScope()) {
        scope.launch {
            delay(delay)
            produceEvent(event)
        }
    }
}

@Keep
open class Event(val value: Int) {}
```

事件发射和订阅：

        lifecycleScope.launch {
            EventBus.getEventFlow(Event::class.java).collect {
                Log.e("Flow", "EventBus Collect: value=${it.value}")
            }
        }
        EventBus.postEvent(Event(1), 0, lifecycleScope)
        EventBus.postEvent(Event(2), 0)
        
    控制台输出结果：
    Flow                    com.example.wangjaing                E  EventBus Collect: value=1
    Flow                    com.example.wangjaing                E  EventBus Collect: value=2

使用 SharedFlow 来做事件总线，有以下优点：

1. 事件可以延迟发送
2. 可以定义粘性事件
3. 事件可以感知 Activity 或 Fragment 的生命周期
4. 事件是有序的

### SharedFlow总结

在热流 SharedFlow 中，当它创建以后它就存在了，它可以在生产者 emit 数据时，没有消费者 collect 数据而独立运行。当生产者 emit 数据后，这些数据会被缓存下来，新老消费者都可以收到这些数据，从而达到共享数据。

对于发射数据操作，会受到 MutableSharedFlow 构造方法参数 replay， extraBufferCapacity，onBufferOverflow 值的影响，这些参数会决定发射操作是挂起还是不挂起。发射的数据，将使用缓存数组进行管理，管理区域分为 buffered values 和 queued emitters。replay 和extraBufferCapacity 参数决定了buffered values 区域的大小，当 buffered values 区域存满溢出时，会根据溢出策略 onBufferOverflow 进行区域调整。当 replay=0 和 extraBufferCapacity=0 ，或 replay!=0 和 extraBufferCapacity!=0 且 buffered values 区域存满， 发射的数据将被包装成 Emitter 存储到 queued emitters 区域。另外，订阅者数量决定了发射数据是存储到缓存区还是丢弃。最后，缓存区存储的数据对所有订阅者共享。

对于收集数据操作，使用 slots: Array<SharedFlowSlot?> 数组来管理订阅者，其中每一个 slot 对象对应一个订阅者，slot 对象的 slot.index 将订阅者要收集的数据与缓存区关联起来，slot.cont 将订阅者所在协程与 SharedFlow上下文关联起来。如果通过 slot.index 能在缓存区取到值，就直接将值给订阅者。否则就将订阅者封装成 Continuation 接口实现类对象存储到 slot.cont 中，挂起订阅者所在协程，等待缓存区有值时，再恢复订阅者协程并给它值。当订阅者协程不存活时，会释放订阅者关联的 slot 对象，也就是重置 slot.inext 和 slot.cont 的值，并重新调整缓存数组的位置。

`tryEmit`会将发射的结果回调，并且如果缓冲区策略配置为suspend时会将这次数据的发射挂起，并将结果返回false，当缓冲区有空间时再进行发射。

`emit` 当缓冲区没有空间时，该操作就会挂起



### StateFlow业务场景

在业务中可以用来做**状态更新**（替换 LiveData）。

比如从服务端获取一个列表数据，并把列表数据展示到 UI。下面使用 `MVI (Model-View-Intent）`来做：

Data Layer：

```
class FlowRepository private constructor() {

    companion object {
        @JvmStatic
        fun newInstance(): FlowRepository = FlowRepository()
    }

    fun requestList(): Flow<List<ItemBean>> {
        val call = ServiceGenerator
            .createService(FlowListApi::class.java)
            .getList()
        return flow {
            emit(call.execute())
        }.flowOn(Dispatchers.IO).filter { it.isSuccessful }
            .map {
                it.body()?.data
            }
            .filterNotNull().catch {
                emit(emptyList())
            }.onEmpty {
                emit(emptyList())
            }
    }
}
```

ViewModel：

```
class ListViewModel : ViewModel() {

    private val repository: FlowRepository = FlowRepository.newInstance()
    
    private val _uiIntent: Channel<FlowViewIntent> = Channel()
    private val uiIntent: Flow<FlowViewIntent> = _uiIntent.receiveAsFlow()
    
    private val _uiState: MutableStateFlow<FlowViewState<List<ItemBean>>> =
        MutableStateFlow(FlowViewState.Init())
    val uiState: StateFlow<FlowViewState<List<ItemBean>>> = _uiState

    fun sendUiIntent(intent: FlowViewIntent) {
        viewModelScope.launch {
            _uiIntent.send(intent)
        }
    }

    init {
        viewModelScope.launch {
            uiIntent.collect {
                handleIntent(it)
            }
        }
    }

    private fun handleIntent(intent: FlowViewIntent) {
        viewModelScope.launch {
            repository.requestList().collect {
                if (it.isEmpty()) {
                    _uiState.emit(FlowViewState.Failure(0, "data is invalid"))
                } else {
                    _uiState.emit(FlowViewState.Success(it))
                }
            }
        }
    }
}


data class FlowViewIntent()

sealed class FlowViewState<T> {
    @Keep
    class Init<T> : FlowViewState<T>()

    @Keep
    class Success<T>(val result: T) : FlowViewState<T>()

    @Keep
    class Failure<T>(val code: Int, val msg: String) : FlowViewState<T>()
}
```

UI：

```
 private var isRequestingList = false
 private lateinit var listViewModel: ListViewModel

 private fun initData() {
        listViewModel = ViewModelProvider(this)[ListViewModel::class.java]
        lifecycleScope.launchWhenStarted {
            listViewModel.uiStateFlow.collect {
                when (it) {
                    is FlowViewState.Success -> {
                        showList(it.result)
                    }
                    is FlowViewState.Failure -> {
                        showListIfFail()
                    }
                    else -> {}
                }
            }
        }
        requestList()
    } 

  private fun requestList() {
        if (!isRequestingList) {
            isRequestingList = true
            listViewModel.sendUiIntent( FlowViewIntent() )
        }
    }
```

使用 StateFlow 替换 LiveData ，并用结合 MVI 替换 MVVM 后，可以有以下优点：

1. **唯一可信数据源**：MVVM 中 可能会存在大量 LiveData，这导致数据交互或并行更新出现逻辑不可控，添加 UIState 结合 StateFlow ，数据源只有 UIState；
2. **数据单向流动**：MVVM 中存在数据 UI ⇆ ViewModel 相互流动，而 MVI 中数据只能从 Data Layer → ViewModel → UI 流动，数据是单向流动的。

使用 StateFlow 替换 LiveData 来做事件状态更新，有以下区别：

> - StateFlow 需要将初始状态传递给构造函数，而 LiveData 不需要。
> - 当 View 进入 STOPPED 状态时，LiveData.observe() 会自动取消注册使用方，而从 StateFlow 或任何其他数据流收集数据的操作并不会自动停止。如需实现相同的行为，需要从 Lifecycle.repeatOnLifecycle 块收集数据流。

### StateFlow总结

热流 StateFlow，基于 SharedFlow 实现，所以它也有独立存在和共享的特点。但在 StateFlow 中发射数据，只有最新的值被缓存下来，所以当新老订阅者订阅时，只会收到它最后一次更新的值，如果发射的新值和当前值相等，订阅者也不会收到通知(防抖)。



## 11.shareIn和stateIn

### 1.冷流转换热流  

在协程中，通过调用操作符shareIn与stateIn，可以将一个冷流转换成一个热流，这两个方法的区别如下：

- **shareIn**：将一个冷流转换成一个标准的热流——SharedFlow类型的对象。
- **stateIn**：将一个冷流转换成一个单数据更新的热流——StateFlow类型的对象。

  ```
  public fun <T> Flow<T>.shareIn(
      scope: CoroutineScope,
      started: SharingStarted,
      replay: Int = 0
  ): SharedFlow<T> {
      ...
  }
  ```
  
  shareIn方法与stateIn方法的使用与实现的原理类似，下面以shareIn方法为例进行分析。

### 2.热流的控制指令

  热流的三个启动终止策略分别对应StartedEagerly、StartedLazily、StartedWhileSubscribed这三个类的对象。除了三个启动终止策略外，接口中还定义了一个核心方法command，用于将SharedFlow类型对象的全局变量subscriptionCount，转换为泛型SharingCommand的Flow类型对象，实际上就是通过监听订阅者数量的变化来发出不同的控制指令。

  StartedEagerly、StartedLazily、StartedWhileSubscribed这三个类都实现了SharingStarted接口，并重写了command方法。如果我们需要自定义一个新的启动终止策略，也可以通过实现SharingStarted接口重写command方法来完成。

#### 1.热流的控制指令

  SharingCommand类是一个枚举类，定义了控制热流的指令，代码如下：

```kotlin
kotlin

复制代码public enum class SharingCommand {
    // 启动热流，并触发上游流的执行
    START,

    // 终止热流，并取消上游流的执行
    STOP,

    // 终止热流，并取消上游流的执行，同时将replayCache重置为初始状态
    // 如果热流的类型为StateFlow，则将replayCache重置为初始值
    // 如果热流的类型为SharedFlow，则调用resetReplayCache方法，清空replayCache
    STOP_AND_RESET_REPLAY_CACHE
}
```

  连续发射相同的指令不会有任何作用。先发射STOP指令，再发射START指令，可以触发热流的重启，并重新触发上游流的执行。

#### 2.StartedEagerly策略的实现

  StartedEagerly策略表示立刻启动热流，并且不会终止，由StartedEagerly类实现，代码如下：

```kotlin
kotlin

复制代码private class StartedEagerly : SharingStarted {
    override fun command(subscriptionCount: StateFlow<Int>): Flow<SharingCommand> =
        flowOf(SharingCommand.START)
        
    override fun toString(): String = "SharingStarted.Eagerly"
}

...


public fun <T> flowOf(value: T): Flow<T> = flow {
    emit(value)
}
```

  Eagerly策略不关心订阅者的数量，在触发后直接向下游发射START指令。

#### 3.StartedLazily策略的实现

  Lazily策略表示当第一个订阅者出现时启动热流，并且只会发射一次，由StartedLazily类实现，代码如下：

```kotlin
kotlin

复制代码private class StartedLazily : SharingStarted {
    override fun command(subscriptionCount: StateFlow<Int>): Flow<SharingCommand> = flow {
        // 标志位，默认为false
        var started = false
        // 监听订阅者数量的变化
        subscriptionCount.collect { count ->
            // 如果订阅者数量大于0，且之前没有发射过指令
            if (count > 0 && !started) {
                // 设置标志位为true
                started = true
                // 发射START指令
                emit(SharingCommand.START)
            }
        }
    }

    override fun toString(): String = "SharingStarted.Lazily"
}
```

  Lazily策略只有当订阅者数量大于0的时候，才会向下游发射START指令，并且只会发射一次。

#### 4.WhileSubscribed策略的实现

  WhileSubscribed策略默认情况下表示当第一个订阅者出现时启动热流，并在最后一个订阅者消失时终止，保留replayCache中的数据，由StartedWhileSubscribed类实现，代码如下：

```kotlin
kotlin

复制代码private class StartedWhileSubscribed(
    private val stopTimeout: Long,
    private val replayExpiration: Long
) : SharingStarted {

    ...

    override fun command(subscriptionCount: StateFlow<Int>): Flow<SharingCommand> = 
        // 监听订阅者变化，并对上游发射的数据进行转换
        subscriptionCount.transformLatest { count ->
            // 如果订阅者数量大于0
            if (count > 0) {
                // 发射START指令
                emit(SharingCommand.START)
            } else { // 如果订阅者数量等于0
                // 延迟指定的热流终止时间
                delay(stopTimeout)
                // 如果指定的清除缓存时间大于0
                if (replayExpiration > 0) {
                    // 发射STOP指令
                    emit(SharingCommand.STOP)
                    // 延迟指定的清除缓存时间
                    delay(replayExpiration)
                }
                // 发射STOP_AND_RESET_REPLAY_CACHE指令
                emit(SharingCommand.STOP_AND_RESET_REPLAY_CACHE)
            }
        } // 只有当START指令发射后，才会向下游发射
        .dropWhile { it != SharingCommand.START }
        .distinctUntilChanged()// 只有当前后指令不同时，才会向下游发射

    ...
}
```

  WhileSubscribed策略在订阅者数量大于0的时候向下游发射START指令，在订阅者数量等于0的时候根据不同的延迟时间参数向下游发射STOP指令和STOP_AND_RESET_REPLAY_CACHE指令。并且必须先发射START指令，相邻重复的指令也不会被发射到下游。



### 3.shareIn 和 stateIn 使用须知

#### 使用 shareIn 与 stateIn 优化 locationsSource 数据流:

使用底层数据流生产者发出位置更新。它是一个使用 [callbackFlow](https://link.juejin.cn/?target=https%3A%2F%2Fkotlin.github.io%2Fkotlinx.coroutines%2Fkotlinx-coroutines-core%2Fkotlinx.coroutines.flow%2Fcallback-flow.html) 实现的 **冷流**。每个新的收集者都会触发数据流的生产者代码块，同时也会将新的回调加入到 FusedLocationProviderClient。

```
class LocationDataSource(
    private val locationClient: FusedLocationProviderClient
) {
    val locationsSource: Flow<Location> = callbackFlow<Location> {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result ?: return
                try { offer(result.lastLocation) } catch(e: Exception) {}
            }
        }
        requestLocationUpdates(createLocationRequest(), callback, Looper.getMainLooper())
            .addOnFailureListener { e ->
                close(e) // in case of exception, close the Flow
            }
        // 在 Flow 结束收集时进行清理
        awaitClose {
            removeLocationUpdates(callback)
        }
    }
}
```

让我们看看在不同的用例下如何使用 shareIn 与 stateIn 优化 locationsSource 数据流。

StateFlow 是 SharedFlow 的一种特殊配置，旨在优化分享状态: 最后被发送的项目会重新发送给新的收集者.

两者之间的最主要区别，在于 `StateFlow` 接口允许您通过读取 `value` 属性同步访问其最后发出的值。而这不是 `SharedFlow` 的使用方式。

#### **提升性能:**

通过共享所有收集者要观察的同一数据流实例 (而不是按需创建同一个数据流的新实例)，这些 API 可以为我们提升性能。

在下面的例子中，`LocationRepository` 消费了 `LocationDataSource` 暴露的 `locationsSource` 数据流，同时使用了 shareIn 操作符，从而让每个对用户位置信息感兴趣的收集者都从同一数据流实例中收集数据。这里只创建了一个 `locationsSource` 数据流实例并由所有收集者共享:

```
class LocationRepository(
    private val locationDataSource: LocationDataSource,
    private val externalScope: CoroutineScope
) {
    val locations: Flow<Location> = 
        locationDataSource.locationsSource.shareIn(externalScope, WhileSubscribed())
}

```

[WhileSubscribed](https://link.juejin.cn?target=https%3A%2F%2Fkotlin.github.io%2Fkotlinx.coroutines%2Fkotlinx-coroutines-core%2Fkotlinx.coroutines.flow%2F-while-subscribed.html) 共享策略用于在没有收集者时取消上游数据流。这样一来，我们便能在没有程序对位置更新感兴趣时避免资源的浪费。

> **Android 应用小提醒！** 在大部分情况下，您可以使用 **WhileSubscribed(5000)**，当最后一个收集者消失后再保持上游数据流活跃状态 5 秒钟。这样在某些特定情况 (如配置改变) 下可以避免重启上游数据流。当上游数据流的创建成本很高，或者在 ViewModel 中使用这些操作符时，这一技巧尤其有用。



#### **缓冲事件**:

在下面的例子中，我们的需求有所改变。现在要求我们保持监听位置更新，同时要在应用从后台返回前台时在屏幕上显示最后的 10 个位置:

```
class LocationRepository(
    private val locationDataSource: LocationDataSource,
    private val externalScope: CoroutineScope
) {
    val locations: Flow<Location> = 
        locationDataSource.locationsSource
            .shareIn(externalScope, SharingStarted.Eagerly, replay = 10)
}
```

我们将参数 `replay` 的值设置为 10，来让最后发出的 10 个项目保持在内存中，同时在每次有收集者观察数据流时重新发送这些项目。为了保持内部数据流始终处于活跃状态并发送位置更新，我们使用了共享策略 `SharingStarted.Eagerly`，这样就算没有收集者，也能一直监听更新。

#### **缓存数据:**

我们的需求再次发生变化，这次我们不再需要应用处于后台时 *持续* 监听位置更新。不过，我们需要缓存最后发送的项目，让用户在获取当前位置时能在屏幕上看到一些数据 (即使数据是旧的)。针对这种情况，我们可以使用 stateIn 操作符。

```Kotlin
Kotlin

复制代码class LocationRepository(
    private val locationDataSource: LocationDataSource,
    private val externalScope: CoroutineScope
) {
    val locations: Flow<Location> = 
        locationDataSource.locationsSource.stateIn(externalScope, WhileSubscribed(), EmptyLocation)
}
```

`Flow.stateIn` 可以缓存最后发送的项目，并重放给新的收集者。



#### **注意！不要在每个函数调用时创建新的实例:**

**切勿** 在调用某个函数调用返回时，使用 shareIn 或 stateIn 创建新的数据流。这样会在每次函数调用时创建一个新的 SharedFlow 或 StateFlow，而它们将会一直保持在内存中，直到作用域被取消或者在没有任何引用时被垃圾回收。

```Kotlin
Kotlin

复制代码class UserRepository(
    private val userLocalDataSource: UserLocalDataSource,
    private val externalScope: CoroutineScope
) {
    // 不要像这样在函数中使用 shareIn 或 stateIn 
    // 这将在每次调用时创建新的 SharedFlow 或 StateFlow，而它们将不会被复用。
    fun getUser(): Flow<User> =
        userLocalDataSource.getUser()
            .shareIn(externalScope, WhileSubscribed())    

    // 可以在属性中使用 shareIn 或 stateIn 
    val user: Flow<User> = 
        userLocalDataSource.getUser().shareIn(externalScope, WhileSubscribed())
}
```

#### **需要入参的数据流**

需要入参 (如 `userId`) 的数据流无法简单地使用 `shareIn` 或 `stateIn` 共享。以开源项目——Google I/O 的 Android 应用 [iosched](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2Fgoogle%2Fiosched) 为例，您可以在 [源码中](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2Fgoogle%2Fiosched%2Fblob%2Fmain%2Fshared%2Fsrc%2Fmain%2Fjava%2Fcom%2Fgoogle%2Fsamples%2Fapps%2Fiosched%2Fshared%2Fdata%2Fuserevent%2FFirestoreUserEventDataSource.kt%23L107) 看到，从 [Firestore](https://link.juejin.cn?target=https%3A%2F%2Ffirebase.google.com%2Fdocs%2Ffirestore%2Fquickstart) 获取用户事件的数据流是通过 `callbackFlow` 实现的。由于其接收 `userId` 作为参数，因此无法简单使用 `shareIn` 或 `stateIn` 操作符对其进行复用。

```Kotlin
Kotlin

复制代码class UserRepository(
    private val userEventsDataSource: FirestoreUserEventDataSource
) {
    // 新的收集者会在 Firestore 中注册为新的回调。
    // 由于这一函数依赖一个 `userId`，所以在这个函数中
    // 数据流无法通过调用 shareIn 或 stateIn 进行复用.
    // 这样会导致每次调用函数时，都会创建新的  SharedFlow 或 StateFlow
    fun getUserEvents(userId: String): Flow<UserEventsResult> =
        userLocalDataSource.getObservableUserEvents(userId)
}
```

如何优化这一用例取决于您应用的需求:

- 您是否允许同时从多个用户接收事件？如果答案是肯定的，您可能需要为 `SharedFlow` 或 `StateFlow` 实例创建一个 map，并在 `subscriptionCount` 为 0 时移除引用并退出上游数据流。
- 如果您只允许一个用户，并且收集者需要更新为观察新的用户，您可以向一个所有收集者共用的 `SharedFlow` 或 `StateFlow` 发送事件更新，并将公共数据流作为类中的变量。

`shareIn` 与 `stateIn` 操作符可以与冷流一同使用来提升性能，您可以使用它们在没有收集者时添加缓冲，或者直接将其作为缓存机制使用。小心使用它们，不要在每次函数调用时都创建新的数据流实例——这样会导致资源的浪费及预料之外的问题！



### 4.shareIn 转换深入



可以使用 shareIn 方法把普通 flow 冷流转化成 SharedFlow 热流。通过 shareIn 创建的 SharedFlow 会把数据供给 View 订阅，同时也会订阅上游的数据流：

```
public fun <T> Flow<T>.shareIn(
    scope: CoroutineScope,
    started: SharingStarted,
    replay: Int = 0
): SharedFlow<T>
```

有三个参数：

- scope: 用于共享数据流的 CoroutineScope, 此作用域函数的生命周期应长于任何使用方，使共享数据流在足够长的时间内保持活跃状态。
- started: 启动策略
- replay: 同上 replay 含义

started 有三种取值：

- Eagerly: 立即启动，到 scope 作用域被结束时停止
- Lazily: 当存在首个订阅者时启动，到 scope 作用域被结束时停止
- WhileSubscribed: 在没有订阅者的情况下取消订阅上游数据流，避免不必要的资源浪费

对于只执行一次的操作，可以使用 Lazily 或 Eagerly, 否则可以使用 WhileSubscribed 来实现一些优化。

它支持两个参数：

```
public fun WhileSubscribed(
    stopTimeoutMillis: Long = 0,
    replayExpirationMillis: Long = Long.MAX_VALUE
): SharingStarted

```

- stopTimeoutMillis: 最后一个订阅者结束订阅后多久停止订阅上游流

- replayExpirationMillis: 数据 replay 的过时时间，超出时间后，新订阅者不会收到历史数据. 这个参数指定一个再停止流执行和清除流缓存的时间间隔，也就是当停止流执行后，间隔`replayExpirationMillis`ms去清楚流的缓存。



#### 简单示例1:

```
val flow12 = flow<Int> {
    repeat(100) {                                                                           
        delay(20)                                                                       
        emit(cur)                                                                       
        println("flow12 emit data: ${cur++}")   
    }                                                                                                           
}                                                                                                                  
/**                                                        
 * started 有三种取值：                                                                                          
 * Eagerly: 立即启动，到 scope 作用域被结束时停止                   
 * Lazily: 当存在首个订阅者时启动，到 scope 作用域被结束时停止               
 * WhileSubscribed: 在没有订阅者的情况下取消订阅上游数据流，避免不必要的资源浪费   
 */                                                                                binding.button12.setOnClickListener {                                          
    //设置SharingStarted.Eagerly后, 上面的repeat方法会立即启动然后开始emit数据  
    flow12.shareIn(lifecycleScope, SharingStarted.Eagerly, 10)      
}
```

#### 简单示例2:

**Repository 层**：

```Kotlin
Kotlin

复制代码class MainRepository {
    private val _data: MutableSharedFlow<Int> = MutableSharedFlow()
    val data: SharedFlow<Int> = _data

    suspend fun request() {
        var cur = 1
        repeat(100) {
            delay(100)
            _data.emit(cur) // 模拟生产数据
            println("emit data: ${cur++}")
        }
    }
}
```

**ViewModel 层**：

```Kotlin
Kotlin

复制代码class MainViewModel(
    private val repository: MainRepository
) : CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {
    val data by lazy {
        repository.data.shareIn(
            scope = this,
            started = SharingStarted.WhileSubscribed(5000)
        ).onEach {
            println("repository data: $it")
        }.map {
            // 转换数据
            "UI Data-$it"
        }
    }
    private val _state = MutableStateFlow(MainState.INIT)
    val state: StateFlow<MainState> = _state

    fun refresh() {
        launch {
            _state.value = MainState.LOADING
            repository.request()
            _state.value = MainState.INIT
        }
    }
}
```

**View 层**：

```Kotlin
Kotlin

复制代码jobData = launch {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.data.collect {
            println("data: $it")
        }
    }
}
jobState = launch {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect {
            println("state: $it")
        }
    }
}
```



### 5.stateIn转换

和 SharedFlow 类似，也可以用 stateIn 将普通流转化成 StateFlow:

```Kotlin
Kotlin

复制代码public fun <T> Flow<T>.stateIn(
    scope: CoroutineScope,
    started: SharingStarted,
    initialValue: T
): StateFlow<T>
```

跟 shareIn 不同的是需要传入一个初始值。

### 6.比较好的文章

Flow 操作符 shareIn 和 stateIn 使用须知 https://juejin.cn/post/6998066384290709518



## 12.Flow 与 RxJava

`Flow` 和 `RxJava` 的定位很接近，罗列一下它们的对应关系：

- `Flow` = (cold冷流) `Flowable` / `Observable` / `Single`
- `Channel` = `Subjects`
- `StateFlow` = `BehaviorSubjects` (永远有值)
- `SharedFlow` = `PublishSubjects` (无初始值)
- `suspend function` = `Single` / `Maybe` / `Completable`





## 13.StateFlow Channel  SharedFlow 特性对比

### 1. StateFlow 的特性

StateFlow 是一种特殊的 Flow，它用于持有状态。它总是有一个初始值，并且只会在状态有变化时发射新的值。它是热流（Hot Flow），意味着当有多个收集器时，它们会共享同一个状态，并且只有最新的状态会被发射。适用于表示UI状态，因为UI总是需要知道当前的状态是什么。

**为什么页面的状态用StateFlow来发送和监听，有什么道理吗？**

1. 状态保留 StateFlow 自动保持其最新值的状态。这意味着每当有新的收集者开始收集此流时，它会立即接收到最新状态的最新值。这对于 UI 编程尤其重要，因为您通常希望 UI 组件（如Activity、Fragment或View）能够立即反映当前的状态，即使它们在状态更新后才开始观察状态。
2. 去重 StateFlow 仅在状态发生变化时通知收集者。如果您向 StateFlow 发射一个与当前值相同的值，这个值将不会被重新发射给收集者。这有助于减少不必要的 UI 更新和性能开销，因为您的 UI 组件不会对相同的状态重复渲染。
3. 线程安全 StateFlow 的操作是线程安全的，确保即使在并发环境中，状态的更新和读取也保持一致性。在复杂的应用程序中，可能有多个协程同时尝试更新状态，StateFlow 保证了这种操作的正确性。

**使用默认的SharedFlow 和 普通的Flow冷流不行吗？**

- SharedFlow：虽然 SharedFlow 可以高度自定义，包括配置重播和缓存策略，但它不保证自动保持和重放最新状态。如果使用 SharedFlow，您需要手动管理状态的保留和更新，这增加了复杂性。
- 普通 Flow：普通的 Flow 是一个冷流，意味着它不保持状态，并且每次有新的收集者时，数据的产生逻辑都会从头开始。这使得它不适合作为表示 UI 状态的机制，因为您通常希望即使在数据生产后也能让后来的收集者立即获得最新状态。

### 2. Channel 的特性

Channel 类似于阻塞队列，但它是挂起的，适用于协程之间的通信。它可以配置为不同的模式，如缓存大小和发送行为。适用于事件的生产者-消费者模型，如任务执行、消息传递等。

**为什么 sendUiIntent 中接收 UI 的驱动事件要用 Channel 来发送和监听，有什么道理吗？**

1. 事件的即时性和一次性 Channel 被设计为用于通信的原语，特别适合于处理一次性事件或命令，这些事件或命令通常不需要被重复消费或保留状态。在 UI 交互中，用户的操作（如点击、滑动等）往往是即时和一次性的，Channel 能够有效地传递这些即时事件，确保它们被及时处理。
2. 缓冲和背压管理 Channel 提供了不同的缓冲策略，包括无限缓冲（Channel.UNLIMITED）、有界缓冲和不缓冲（Channel.RENDEZVOUS）。这使得开发者可以根据具体的应用场景选择最合适的策略来处理事件流，例如，通过使用无限缓冲，可以确保在高频事件发生时不会丢失任何事件。
3. 明确的消费模式 与 Flow 相比，Channel 提供了更明确的消费模式，即发送方和接收方。这种模式使得事件的发送和接收更加直观，尤其是在需要明确处理每个事件的场景中。此外，Channel 的 send 和 receive 操作可以很容易地集成到协程中，提供了更灵活的并发处理能力。

**使用默认的SharedFlow 和 StateFlow，普通的Flow冷流不行吗？**

- SharedFlow：虽然 SharedFlow 可以用于处理事件，并且支持配置重播和并发策略，但它更适合于需要多个观察者共享和重播事件的场景。对于一次性的、即时的 UI 事件，SharedFlow 的这些特性可能并不是必需的。
- StateFlow：StateFlow 主要用于表示和观察可变状态，它保留最新的状态值并且只在状态变化时通知观察者。这种特性使得 StateFlow 不适合用于传递一次性的 UI 事件。
- 普通 Flow：普通的 Flow 是一个冷流，它不保留状态或事件，而是在每次收集时重新开始生成数据。这种特性使得它不适合于处理即时的 UI 事件，因为事件可能会在观察者开始收集之前发生并且丢失。

综上所述，Channel 在处理即时和一次性的 UI 事件方面提供了特定的优势，尤其是在需要明确的事件发送和接收、以及灵活的缓冲和背压管理时。这些特性使得 Channel 成为在特定场景下处理 UI 事件的理想选择。

### 3. SharedFlow 的特性

SharedFlow 也是一种热流，能够向多个收集器广播事件。它提供了更灵活的配置，比如可以配置重播(replay)的值的数量，以及在没有收集器的情况下保留值的能力。适用于一次性事件、消息广播等场景。

**默认的 SharedFlow**

**为什么UI的效果通知要用 SharedFlow 来发送和监听，有什么道理吗？**

1. 多观察者支持 与 StateFlow 和普通的 Flow 相比，SharedFlow 支持多个观察者同时订阅事件，而且每个观察者都会收到独立的事件流。这一点对于 UI 事件非常重要，因为可能有多个组件或功能同时对同一事件感兴趣，并且需要独立处理这些事件。
2. 事件重播和缓存策略 SharedFlow 可以配置事件的重播 (Replay) 和缓存 (Buffer) 策略。这意味着你可以控制新订阅者接收多少最近的事件，或者当流的发射速度超过处理速度时如何缓存事件。这在处理 UI 事件时非常有用，例如，当你希望新加入的观察者能够接收到最近的状态或事件时。
3. 精细的背压管理 尽管 Channel 也提供了背压管理，但 SharedFlow 允许更精细的背压控制，特别是在配置缓存和重播行为时。这对于确保 UI 事件不会因为过载而丢失或导致性能问题非常关键。

**使用Channel 和 StateFlow，普通的Flow冷流不行吗？**

- Channel：虽然 Channel 适用于一次性事件和瞬时通信，但其主要设计用于协程之间的通信，而不是状态管理或多观察者场景。Channel 的每个事件只能被一个观察者消费，这限制了其在 UI 事件广播中的使用。
- StateFlow：StateFlow 适用于状态管理，因为它总是保留最新的状态值，并且能够向新订阅者重播这个状态。然而，它不适合用于表示可以发生多次的一次性事件，如点击事件。
- 普通 Flow：普通的 Flow 是一个冷流，它只有在收集时才开始发射数据。这意味着它不适合用于事件的多订阅者广播或需要重播最近事件给新订阅者的场景。

综上所述，SharedFlow 在用于 UI 效果通知时提供了对多观察者支持、可配置的重播和缓存策略以及精细的背压管理，这些特性使得它成为处理这些场景的理想选择。

**配置版 SharedFlow**

SharedFlow 是 Kotlin 协程中最强大的 Flow 实现，他有很多的配置，具有最大的灵活性和可定制性。我们可以通过设置不同的参数和处理策略，来模拟 StateFlow 和 Channel 的行为。

**模拟 StateFlow**

StateFlow 持有一个值，并且只在值改变时通知观察者。要用 SharedFlow 模拟这个行为，我们可以配置它的重播缓存(replay cache)大小为 1，并且设置它的行为，使它只在值改变时发射数据。

```ini
ini

复制代码val sharedFlowState = MutableSharedFlow<Int>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
).apply {
    tryEmit(initialValue)  // 初始化SharedFlow的值
}

// 对比StateFlow
val stateFlow = MutableStateFlow(initialValue)
```

使用 tryEmit 来设置初始值，这样就模拟了 StateFlow 的行为。接下来，您需要确保只有在值改变时才调用 emit 方法。

**模拟 Channel**

Channel 用于协程间的通信，并且可以配置为有不同的容量。用 SharedFlow 来模拟 Channel，您可以设置它的重播值为 0 并配置缓存策略。

```ini
ini

复制代码val sharedFlowChannel = MutableSharedFlow<Int>(
    replay = 0,
    extraBufferCapacity = Channel.BUFFERED.capacity, // 或者设置具体的数值
    onBufferOverflow = BufferOverflow.SUSPEND
)

// 对比Channel
val channel = Channel<Int>(Channel.BUFFERED)
```

示例：

```kotlin
kotlin

复制代码import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// 模拟StateFlow
fun simulateStateFlow() = MutableSharedFlow<Int>(replay = 1)

// 模拟Channel
fun simulateChannel() = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1(Int.MAX_VALUE, // 无限缓冲区), onBufferOverflow = BufferOverflow.SUSPEND)

fun main() = runBlocking {
    val simulatedStateFlow = simulateStateFlow()
    simulatedStateFlow.tryEmit(1) // 设置初始值

    launch {
        simulatedStateFlow.collect { value ->
            println("StateFlow simulated: Received $value")
        }
    }
    simulatedStateFlow.emit(2) // 发送新值
    simulatedStateFlow.emit(2) // 发送相同的值，不会再次通知

    val simulatedChannel = simulateChannel()

    launch {
      	//在接收的时候加distinctUntilChanged()去重
        simulatedChannel.distinctUntilChanged().collect { value ->
            println("Channel simulated: Received $value")
        }
    }
    simulatedChannel.emit(1) // 发送值
    simulatedChannel.emit(2) // 发送另一个值

    delay(1000) // 等待收集
}
```

### 后记:

本文我们分别说明普通Flow，StateFlow，SharedFlow，Channel的特性和他们的差异，以及为什么 MVI 场景下要选用对应的 Flow 来完成框架。

具体的分析可以在文中查看，那么我们接下来再看一个问题，如果此时 Activity 销毁重建，那么Channel 的驱动事件，StateFlow的UI状态，和SharedFlow 的UI效果，会有怎样的效果呢？

在 Android 应用中，Activity 的销毁和重建（例如，由于配置更改）会对使用 Channel、StateFlow 和 SharedFlow 的事件和状态管理产生不同的影响。我们可以分别讨论它们的行为：

1. StateFlow（UI状态） StateFlow 保持其状态，即便是当 Activity 销毁并重建。这意味着新的 Activity 实例订阅 uiStateFlow 时，将立即接收到当前的状态，也就是获取了当前状态的一个快照，确保 UI 正确反映了最新的状态。所以我们会根据UI状态刷新对应的布局展示，符合我们的预期。
2. Channel（驱动事件） Channel 用于处理一次性的事件或者命令，它是一个热流，意味着一旦事件被发送并被收集，该事件就消失了。如果 Activity 在事件发送后被销毁并重建，除非 ViewModel 重新发送事件，否则新的 Activity 实例不会接收到之前的事件。因此不会重新驱动事件，符合我们的预期。
3. SharedFlow（UI效果） 对于 SharedFlow，其行为取决于你对它的配置，尤其是它的重播策略。在 BaseEISViewModel 中，MutableSharedFlow 被初始化时没有指定重播或缓冲策略，因此默认情况下它不会重播旧的事件给新的订阅者。这意味着，在 Activity 重建时，只要没有新的事件被发送，就不会出现重复触发的情况。所以包括页面导航，页面弹窗，吐司等UI效果也不会触发，符合我们的预期。

### 总结:

StateFlow 用于持续状态的管理，保证了状态的一致性，并且默认保存有状态的一个快照，重建之后也能恢复，特别适合 UI 状态的管理。

Channel 主要处理一次性事件，一旦事件被收集，它就不会再次触发，除非显式地重新发送，特别适合 UI 事件的驱动

SharedFlow 由于默认配置并没有配置重播策略，则不会导致重复触发问题，特别适合 UI 效果的管理。



## 14.liveData{}扩展函数

`liveData` 是 AndroidX 提供的一个扩展函数，它允许在 `LiveData` 中使用协程。通过 `liveData` 构建的 `LiveData` 实例，可以在其作用域内使用协程来异步执行代码，并将结果发布到 `LiveData`。

### 函数签名

```kotlin
public fun <T> liveData(
    context: CoroutineContext = EmptyCoroutineContext,
    timeoutInMs: Long = DEFAULT_TIMEOUT,
    @BuilderInference block: suspend LiveDataScope<T>.() -> Unit
): LiveData<T> = CoroutineLiveData(context, timeoutInMs, block)
```

### 参数说明

- `context`: 协程上下文，可选参数，默认为 `EmptyCoroutineContext`。可以传递自定义的 `CoroutineDispatcher`，例如 `Dispatchers.IO`。
- `timeoutInMs`: 超时时间，超时后协程会被取消，默认值为 `DEFAULT_TIMEOUT`。
- `block`: 协程代码块，在 `LiveDataScope` 中执行，可以使用 `emit` 函数发布数据。

### 使用示例

以下是一个使用 `liveData` 构建 `LiveData` 的示例，演示如何在 `ViewModel` 中使用它来执行异步操作，并将结果发布到 `LiveData` 中以供 UI 层观察和使用。

#### 1. 添加依赖

确保你的项目添加了 `lifecycle-livedata-ktx` 依赖：

```groovy
dependencies {
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.3.1"
}
```

#### 2. 创建 ViewModel

在 `ViewModel` 中使用 `liveData` 扩展函数：

```kotlin
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class MyViewModel : ViewModel() {

    val data: LiveData<String> = liveData(Dispatchers.IO) {
        emit("Loading...") // 初始状态
        try {
            val result = fetchDataFromNetwork()
            emit(result) // 成功获取数据
        } catch (e: Exception) {
            emit("Error: ${e.message}") // 处理错误
        }
    }

    private suspend fun fetchDataFromNetwork(): String {
        delay(2000) // 模拟网络请求延迟
        return "Hello, World!"
    }
}
```

#### 3. 在 Activity 或 Fragment 中观察 LiveData

在 `Activity` 或 `Fragment` 中观察 `LiveData` 并更新 UI：

```kotlin
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val viewModel: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 观察 LiveData 并更新 UI
        viewModel.data.observe(this, Observer { data ->
            textView.text = data
        })
    }
}
```

#### 4.解释

1. **ViewModel 中的 `liveData`**:
   - 使用 `liveData` 扩展函数创建 `data`，并指定 `Dispatchers.IO` 作为协程上下文。
   - 在协程代码块中，首先使用 `emit` 函数发布初始状态 "Loading..."。
   - 然后模拟网络请求，通过 `fetchDataFromNetwork` 函数获取数据，并使用 `emit` 函数发布结果。
   - 如果发生异常，捕获异常并使用 `emit` 函数发布错误信息。

2. **Activity 中的 LiveData 观察**:
   - 通过 `by viewModels()` 委托属性获取 `ViewModel` 实例。
   - 使用 `observe` 函数观察 `LiveData`，当数据发生变化时，更新 `TextView` 的文本内容。

这个示例展示了如何使用 `liveData` 扩展函数在 `ViewModel` 中执行异步操作，并将结果发布到 `LiveData`，从而实现数据驱动的 UI 更新。

通过asFlow将liveData转换成flow, 然后使用flowWithLifecycle在生命周期中获取数据

```
lifecycleScope.launch {
    //通过asFlow将liveData转换成flow, 然后使用flowWithLifecycle在生命周期中获取数据
    liveData1.asFlow().flowWithLifecycle(this@Demo170Activity.lifecycle, Lifecycle.State.STARTED)
        .collect{

        }
}
```



协程转换成livaData, 或者是使用Transformations.map转换的时候需要做耗时操作,可以使用liveData{}扩展函数

```
//使用liveData{}扩展函数,处理一些转换时需要的耗时操作

val liveData1 = MutableLiveData<Int>()
val transLiveData = Transformations.map(liveData1) {
    //此处是在主线程中执行, 如果要做一些耗时操作,可以使用下面的liveData{}扩展函数
    //....
    it.toString()
}


val liveData = liveData<Int> {
    //val data = doSuspendingFunction()//协程中处理
    //emit(data)
}
```



### liveData{}中的代码块执行的时机

`liveData {}` 代码块会在 `LiveData` 被激活时执行。`LiveData` 被激活的具体时机是指它有一个活跃的观察者 (active observer)。以下是一些关键点来理解其执行时机：

1. **激活与暂停**：
   - `LiveData` 被激活的时机是当它有至少一个活跃的观察者。这意味着当一个 `LifecycleOwner`（例如 `Activity` 或 `Fragment`）在 `STARTED` 或 `RESUMED` 状态下观察它时，`LiveData` 就会被激活。
   - 当 `LiveData` 没有任何活跃的观察者时，它会被暂停。

2. **协程的启动**：
   - 当 `LiveData` 被激活时，`liveData {}` 代码块中的协程会启动并执行。
   - 如果在执行过程中 `LiveData` 变为非活跃状态，协程会被取消。如果 `LiveData` 再次变为活跃状态，协程会重新启动并从头开始执行。

3. **取消与重启**：
   - `liveData {}` 代码块中的协程支持取消。当 `LiveData` 没有任何活跃的观察者时，协程会被取消。
   - 当 `LiveData` 再次被激活时，协程会重新启动并执行整个代码块。

#### 执行时机示例:

让我们通过一个更详细的示例来理解这一点：

##### ViewModel

```kotlin
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class MyViewModel : ViewModel() {

    val data: LiveData<String> = liveData(Dispatchers.IO) {
        emit("Loading...") // 初始状态
        try {
            val result = fetchDataFromNetwork()
            emit(result) // 成功获取数据
        } catch (e: Exception) {
            emit("Error: ${e.message}") // 处理错误
        }
    }

    private suspend fun fetchDataFromNetwork(): String {
        delay(2000) // 模拟网络请求延迟
        return "Hello, World!"
    }
}
```

##### Activity

```kotlin
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val viewModel: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 观察 LiveData 并更新 UI
        viewModel.data.observe(this, Observer { data ->
            textView.text = data
        })
    }
}
```

##### 代码执行流程

1. **Activity 启动**：
   - 当 `MainActivity` 启动并进入 `onCreate` 方法时，`viewModel.data.observe(this, Observer { ... })` 方法被调用。
   - 由于 `MainActivity` 处于 `STARTED` 或 `RESUMED` 状态，`LiveData` 被激活，并开始执行 `liveData {}` 代码块中的协程。

2. **协程执行**：
   - 协程首先执行 `emit("Loading...")`，将初始状态发布给观察者。
   - 然后，协程调用 `fetchDataFromNetwork()`，模拟一个耗时的网络请求。
   - 请求完成后，协程将结果通过 `emit(result)` 发布给观察者。

3. **Activity 暂停与恢复**：
   - 如果 `MainActivity` 进入后台（例如按下 Home 键），`LiveData` 变为非活跃状态，协程会被取消。
   - 当 `MainActivity` 返回前台，`LiveData` 重新变为活跃状态，协程会重新启动并从头开始执行。

#### 总结

`liveData {}` 代码块中的协程会在 `LiveData` 被激活时执行，并在 `LiveData` 没有任何活跃观察者时取消。如果 `LiveData` 再次被激活，协程会重新启动并从头开始执行。这种机制确保了只有在需要的时候才执行协程代码，从而节省资源。



### 每次有一个新观察者observe这个liveData{}, 那liveData{}里面的方法都会执行一次吗?

不，`liveData {}` 代码块不会为每一个新观察者重复执行。在 Android 的 `LiveData` 机制中，`liveData {}` 代码块只在 `LiveData` 转为活跃状态时执行一次，而不是为每一个新观察者单独执行。

#### 详细解释

1. **单次执行**：`liveData {}` 代码块在 `LiveData` 被激活时只执行一次。这意味着只要 `LiveData` 处于活跃状态，无论有多少个观察者，`liveData {}` 代码块都不会重复执行。

2. **活跃状态**：`LiveData` 被认为是活跃的，当它有至少一个活跃观察者时。如果已经有一个活跃的观察者，`LiveData` 代码块已经在执行或已经执行完成，那么新添加的观察者只是接收已经发布的数据，而不会触发 `liveData {}` 代码块的重新执行。

3. **缓存值**：`LiveData` 会缓存最后一个发布的值，新观察者会立即收到这个值，而不会重新执行 `liveData {}` 代码块。

#### 示例

让我们通过一个示例来更好地理解这一点：

##### ViewModel

```kotlin
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class MyViewModel : ViewModel() {

    val data: LiveData<String> = liveData(Dispatchers.IO) {
        emit("Loading...") // 初始状态
        try {
            val result = fetchDataFromNetwork()
            emit(result) // 成功获取数据
        } catch (e: Exception) {
            emit("Error: ${e.message}") // 处理错误
        }
    }

    private suspend fun fetchDataFromNetwork(): String {
        delay(2000) // 模拟网络请求延迟
        return "Hello, World!"
    }
}
```

##### Activity

```kotlin
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val viewModel: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 第一个观察者
        viewModel.data.observe(this, Observer { data ->
            textView1.text = data
        })

        // 模拟延迟后添加第二个观察者
        textView1.postDelayed({
            viewModel.data.observe(this, Observer { data ->
                textView2.text = data
            })
        }, 3000)
    }
}
```

##### 代码执行流程

1. **Activity 启动**：
   - `MainActivity` 启动并进入 `onCreate` 方法。
   - 第一个观察者通过 `viewModel.data.observe(this, Observer { ... })` 注册。

2. **协程执行**：
   - `LiveData` 被激活，`liveData {}` 代码块开始执行。
   - 协程首先执行 `emit("Loading...")`，将初始状态发布给第一个观察者。
   - 然后，协程调用 `fetchDataFromNetwork()`，模拟耗时的网络请求。
   - 请求完成后，协程将结果通过 `emit(result)` 发布给所有观察者。

3. **第二个观察者**：
   - 延迟 3 秒后，第二个观察者通过 `viewModel.data.observe(this, Observer { ... })` 注册。
   - 因为 `LiveData` 仍然是活跃的，并且之前的协程已经执行完成并发布了结果，所以第二个观察者会立即收到最后一个发布的值（例如 "Hello, World!"），而不会重新触发 `liveData {}` 代码块的执行。

#### 总结

`liveData {}` 代码块在 `LiveData` 被激活时只执行一次，而不是每次新增观察者时都执行。`LiveData` 会缓存最后一个发布的值，新观察者会立即收到这个值，而不会导致 `liveData {}` 代码块的重新执行。这种机制确保了数据处理的高效性和一致性。

### LiveData迁移到StateFlow

https://juejin.cn/post/7071059807456722974

https://segmentfault.com/a/1190000040256135
