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
//todo:这里不是太理解, 后面补充
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
//注意这里异常会被捕获, 但是app还是会crash
//app不会crash
//由于 scope 的直接子协程是 launch，如果 async 中产生了一个异常，这个异常将会被立即抛出。
//原因是 async (包含一个 Job 在它的 CoroutineContext 中) 会自动传播异常到它的父级 (launch)，
//这会让异常被立即抛出。
//输出:
//1
//1------>KotlinNullPointerException
//2------>KotlinNullPointerException
btn9.setOnClickListener {
    jobScope.launch {
        try {
            coroutineScope {
                try {
                    //这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
                    val deferred = async {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()
                    }

                    //这里不执行await也会导致抛出异常,且不被捕获
                    deferred.await()
                } catch (e: Exception) {
                    println("1------>$e")
                }
            }
        } catch (e: Exception) {
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

//todo:不太明白,以后补充
//app不会crash
//输出
// 1
// 1------>KotlinNullPointerException
btn11.setOnClickListener {
  jobScope.launch {
    try {
      supervisorScope {
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
2. repeatOnLifecycle会在当前生命周期大于等于RESUMED的时候执行里面的方法,
   然后小于该生命周期后cancel掉该协程Job



## 8.在使用 Kotlin 协程时应该注意的事情

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

发生这种情况是因为非SupervisorJob的情况下任何子协程的失败都会导致其父协程立即失败。

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
