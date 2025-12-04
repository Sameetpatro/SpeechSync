package com.example.speechsync

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.speechsync.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add fade-in animation
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.loginCard.startAnimation(fadeIn)

        // Setup click listeners
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvCreateAccount.setOnClickListener {
            navigateToSignup()
        }
        binding.btnBypass.setOnClickListener {
            bypassLogin()
        }

    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Basic validation (you can enhance this)
        if (email.isEmpty() || password.isEmpty()) {
            return
        }

        // Navigate to HomeActivity with smooth transition
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        finish()
    }

    private fun bypassLogin() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        finish()
    }


    private fun navigateToSignup() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }


}