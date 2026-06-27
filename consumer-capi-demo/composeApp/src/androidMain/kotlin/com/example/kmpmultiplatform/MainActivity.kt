package com.example.kmpmultiplatform

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = App()
        val tv = TextView(this)
        tv.text = content.text
        tv.setTextColor(Color.parseColor(content.colorHex))
        tv.textSize = content.sizeSp
        tv.typeface = if (content.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val container = FrameLayout(this)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER
        container.addView(tv, lp)
        setContentView(container)
    }
}
