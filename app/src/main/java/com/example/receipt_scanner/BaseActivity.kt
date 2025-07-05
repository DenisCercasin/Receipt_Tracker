package com.example.receipt_scanner

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

abstract class BaseActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        // Inflate your base layout with background
        val baseLayout = layoutInflater.inflate(R.layout.activity_base, null)
        val container = baseLayout.findViewById<FrameLayout>(R.id.baseContainer)

        // Inflate the actual screen layout inside the base container
        layoutInflater.inflate(layoutResID, container, true)

        super.setContentView(baseLayout)
    }
}