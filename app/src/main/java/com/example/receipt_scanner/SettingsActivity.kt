package com.example.receipt_scanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.receipt_scanner.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySettingsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnReturn = findViewById<Button>(R.id.btnReturnDashboard)

        btnResetPassword.setOnClickListener {
            val userEmail = auth.currentUser?.email
            if (userEmail != null) {
                auth.sendPasswordResetEmail(userEmail)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        btnReturn.setOnClickListener {
            finish()
        }
    }
}
