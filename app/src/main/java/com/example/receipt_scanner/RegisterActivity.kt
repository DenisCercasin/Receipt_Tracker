package com.example.receipt_scanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.receipt_scanner.databinding.ActivityRegisterBinding
// All logic similar to login
// Activity responsible for user registration via Firebase Authentication
class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the view using View Binding
        val binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Set the action bar title for this screen
        supportActionBar?.title = "Register"

        // have account -> log in
        binding.loginTV.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        // Handle "Create Account" button click
        binding.createAccountBtn.setOnClickListener{
            val email = binding.emailRegister.text.toString()
            val password = binding.passwordRegister.text.toString()
            // Validate that both fields are not empty
            if (email.isNotEmpty() && password.isNotEmpty())
                // Use Firebase Auth to create a new account
                MainActivity.auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{
                    if(it.isSuccessful){
                        // registration successful → navigate to Dashboard
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                }.addOnFailureListener{
                    // registration failed → show error message
                    Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }


        enableEdgeToEdge()

    }
}