package com.zj.kotlin.coroutine

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 *
 * CreateTime:2022/10/20 17:01
 * @author zhengjiong
 */
class EmptyActivity :AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)


        findViewById<Button>(R.id.btn).setOnClickListener {
            onBackPressed()
        }
    }
}