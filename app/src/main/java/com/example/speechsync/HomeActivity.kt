package com.example.speechsync

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speechsync.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var isRecording = false
    private var selectedLanguage = "Hindi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup language spinner
        setupLanguageSpinner()

        // Setup click listeners
        binding.fabMicrophone.setOnClickListener {
            toggleRecording()
        }

        binding.btnPlayTranslation.setOnClickListener {
            playTranslation()
        }

        binding.btnNewTranslation.setOnClickListener {
            resetTranslation()
        }
    }

    private fun setupLanguageSpinner() {
        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        binding.spinnerLanguage.adapter = adapter

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLanguage = languages[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun toggleRecording() {
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        isRecording = true

        // Update status
        binding.tvStatus.text = getString(R.string.status_listening)

        // Animate microphone button (pulsing effect)
        val scaleX = ObjectAnimator.ofFloat(binding.fabMicrophone, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.fabMicrophone, "scaleY", 1f, 1.2f, 1f)
        scaleX.duration = 1000
        scaleY.duration = 1000
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        scaleX.start()
        scaleY.start()

        // Change button color
        binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.recording)

        // Simulate recording for 3 seconds
        binding.fabMicrophone.postDelayed({
            stopRecording()
        }, 3000)
    }

    private fun stopRecording() {
        isRecording = false

        // Update status
        binding.tvStatus.text = getString(R.string.status_processing)

        // Reset button
        binding.fabMicrophone.clearAnimation()
        binding.fabMicrophone.scaleX = 1f
        binding.fabMicrophone.scaleY = 1f
        binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.primary)

        // Simulate processing
        binding.fabMicrophone.postDelayed({
            displayResults()
        }, 2000)
    }

    private fun displayResults() {
        // Update status
        binding.tvStatus.text = getString(R.string.status_completed)

        // Display sample recognized speech
        binding.tvRecognizedSpeech.text = getString(R.string.sample_recognized)

        // Display sample translation based on selected language
        binding.tvTranslatedOutput.text = when (selectedLanguage) {
            "Hindi" -> "नमस्ते, आप आज कैसे हैं?"
            "Bengali" -> "হ্যালো, আজ আপনি কেমন আছেন?"
            "Odia" -> "ନମସ୍କାର, ଆଜି ଆପଣ କେମିତି ଅଛନ୍ତି?"
            "English" -> "Hello, how are you today?"
            else -> getString(R.string.sample_translated)
        }
    }

    private fun playTranslation() {
        if (binding.tvTranslatedOutput.text.isNotEmpty()) {
            Toast.makeText(this, getString(R.string.playing_translation), Toast.LENGTH_SHORT).show()

            // Animate play button
            val scaleX = ObjectAnimator.ofFloat(binding.btnPlayTranslation, "scaleX", 1f, 0.95f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.btnPlayTranslation, "scaleY", 1f, 0.95f, 1f)
            scaleX.duration = 200
            scaleY.duration = 200
            scaleX.start()
            scaleY.start()
        }
    }

    private fun resetTranslation() {
        // Clear all text fields
        binding.tvRecognizedSpeech.text = ""
        binding.tvTranslatedOutput.text = ""
        binding.tvStatus.text = getString(R.string.status_tap_mic)

        // Reset recording state
        isRecording = false
        binding.fabMicrophone.clearAnimation()
        binding.fabMicrophone.scaleX = 1f
        binding.fabMicrophone.scaleY = 1f
        binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.primary)
    }
}