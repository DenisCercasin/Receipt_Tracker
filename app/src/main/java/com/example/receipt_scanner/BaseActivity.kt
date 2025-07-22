package com.example.receipt_scanner

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

// abstract so wraps every child screen inside a common layout
// so every child nests activity_base.xml → includes → activity_dashboard.xml
abstract class BaseActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        // Inflate your base layout with background from activity_base.xml
        val baseLayout = layoutInflater.inflate(R.layout.activity_base, null)
        val container = baseLayout.findViewById<FrameLayout>(R.id.baseContainer)

        // Inflate the actual screen layout inside the base container
        layoutInflater.inflate(layoutResID, container, true)

        super.setContentView(baseLayout)
    }
}