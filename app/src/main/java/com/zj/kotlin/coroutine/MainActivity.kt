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
            Item("25.Coroutine-协程取消", Coroutine_Cancel_Example::class.java),
            Item("merge", Merge::class.java)
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