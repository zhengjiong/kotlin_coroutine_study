package com.zj.kotlin.coroutine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

/**
 *
 * CreateTime:2020/8/13 10:48
 * @author zhengjiong
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = mutableListOf<Item>(
            Item("1-launch", Demo1Activity::class.java),
            Item("2-withContext", Demo2Activity::class.java),
            Item("3-suspendCancellableCoroutine", Demo3Activity::class.java),
            Item("4-coroutineScope", Demo4Activity::class.java),
            Item("8-协程上下文", Demo8Activity::class.java),
            Item("9-runblocking", Demo9Activity::class.java),
            Item("10-异常处理", Demo10Activity::class.java),
            Item("11-异常处理-2", Demo11Activity::class.java),
            Item("14-异常处理-3", Demo14Activity::class.java),
            Item("25-Coroutine-协程取消", Coroutine_Cancel_Example::class.java),
            Item("66-热流", Demo66Activity::class.java),
            Item("77-repeatOnLifecycle1", Demo77Activity::class.java),
            Item("78-repeatOnLifecycle2", Demo78Activity::class.java),
            Item("88-callbackFlow", Demo88Activity::class.java),
            Item("99-merge", Demo99Activity::class.java),
            Item("109-背压", Demo109Activity::class.java),
            Item("119-StateFlow  ShareFlow", Demo119Activity::class.java),
            Item("130-Sample用法", Demo130Activity::class.java)
        )
        recyclerView.adapter = object : RecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_main, parent, false)
                )
            }

            override fun getItemCount() = list.size

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val item = list[position]
                val btn = holder.itemView.findViewById<TextView>(R.id.title)
                btn.text = item.name
                btn.setOnClickListener {
                    startActivity(Intent(this@MainActivity, item.clazz))
                }
            }

        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    class Item(
        val name: String, val clazz: Class<*>
    )
}