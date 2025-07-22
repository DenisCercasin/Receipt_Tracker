package com.example.receipt_scanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.receipt_scanner.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // extending the oncreate from the appcompatactivity

        // connect your Kotlin code to the layout activity_login.xml.
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // title of the top bar
        supportActionBar?.title = "Login"

        // from login -> no account, register here button
        binding.registerTV.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
            finish() // make sure the user can't press "back" to return to login
        }


        binding.loginBtn.setOnClickListener{
            val email = binding.emailLogin.text.toString()
            val password = binding.passwordLogin.text.toString()
            // basic validation
            if (email.isNotEmpty() && password.isNotEmpty())
                // firebaseauth to attempt a login
                MainActivity.auth.signInWithEmailAndPassword(email,password).addOnCompleteListener{
                    if(it.isSuccessful){
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                }.addOnFailureListener{
                    // toast = small temporary pop up
                    Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }

        // makes the layout draw behind system bars
        enableEdgeToEdge()
        }
    }
