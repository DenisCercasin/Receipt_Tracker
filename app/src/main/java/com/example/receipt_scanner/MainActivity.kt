package com.example.receipt_scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.receipt_scanner.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {
    // binding: Used to access the layout’s buttons/texts (instead of findViewById
    private lateinit var binding: ActivityMainBinding

    // static Firebase Auth instance available from other classes too - like static global
    // now accessible by MainActivity.auth
    companion object{
        lateinit var  auth:FirebaseAuth
    }
    // when the screen starts
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // draws content behind system bars

        auth = FirebaseAuth.getInstance() // initializing the firebase

        // if the user is already logged in, then redirect to dashboard
        if (auth.currentUser != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return // important to stop further execution
        }
        // loads the UI layout activity_main.xml using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setting action to the buttons
        binding.loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Make sure the content is not hidden by the status bar - on the top
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}