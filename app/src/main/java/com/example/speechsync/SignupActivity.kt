package com.example.speechsync

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speechsync.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add fade-in animation
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.signupCard.startAnimation(fadeIn)

        // Setup click listeners
        binding.btnSignup.setOnClickListener {
            createAccount()
        }

        binding.tvAlreadyHaveAccount.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun createAccount() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Basic validation
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Show success message
        Toast.makeText(this, getString(R.string.account_created), Toast.LENGTH_SHORT).show()

        // Navigate back to LoginActivity
        navigateToLogin()
    }

    private fun navigateToLogin() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToLogin()
    }
}