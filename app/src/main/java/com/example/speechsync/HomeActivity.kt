package com.example.speechsync

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    private var selectedInputLang: String? = null
    private var selectedTargetLang: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Set toolbar as ActionBar
        setSupportActionBar(binding.toolbarHome)

        setupLanguageSpinners()
        setupButtons()
    }

    // ---------------------- MENU HANDLING ----------------------

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            R.id.action_sign_out -> {
                performSignOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ---------------------- LANGUAGE SPINNERS ----------------------

    private fun setupLanguageSpinners() {
        val languages = resources.getStringArray(R.array.languages_list).toList()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )

        binding.spinnerInputLanguage.adapter = adapter
        binding.spinnerTargetLanguage.adapter = adapter

        binding.spinnerInputLanguage.setSelection(0)
        binding.spinnerTargetLanguage.setSelection(0)

        binding.spinnerInputLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedInputLang = if (position == 0) null else languages[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.spinnerTargetLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedTargetLang = if (position == 0) null else languages[position]

                    if (selectedInputLang != null &&
                        selectedTargetLang != null &&
                        selectedInputLang == selectedTargetLang
                    ) {
                        Toast.makeText(
                            this@HomeActivity,
                            "Input and target language cannot be the same. Please change target language.",
                            Toast.LENGTH_SHORT
                        ).show()

                        binding.spinnerTargetLanguage.setSelection(0)
                        selectedTargetLang = null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ---------------------- BUTTON SETUP ----------------------

    private fun setupButtons() {
        binding.fabMicrophone.setOnClickListener {
            toggleRecording()
        }

        binding.btnContinueQuestion.setOnClickListener {
            Toast.makeText(
                this,
                "Continue logic will be handled with pause detection later.",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnTranslate.setOnClickListener {
            if (!validateLanguages()) return@setOnClickListener
            startLoadingUI()

            binding.btnTranslate.postDelayed({
                onTranslationReady(durationMs = 5000L)  // fake 5s audio
            }, 2000)
        }

        binding.btnPlayPause.setOnClickListener {
            Toast.makeText(this, "Play/Pause clicked (dummy)", Toast.LENGTH_SHORT).show()
        }

        binding.btnRetry.setOnClickListener {
            binding.tvStatus.text = getString(R.string.status_tap_mic)
            binding.tvAudioStatus.text = "Waiting for translation..."
            binding.tvAudioDuration.text = "00:00"
            binding.progressLoading.visibility = View.GONE
            Toast.makeText(this, "Retry: please record your voice again.", Toast.LENGTH_SHORT)
                .show()
        }

        binding.btnSwapLanguages.setOnClickListener {
            val inputPos = binding.spinnerInputLanguage.selectedItemPosition
            val targetPos = binding.spinnerTargetLanguage.selectedItemPosition

            if (inputPos != 0 && targetPos != 0) {
                binding.spinnerInputLanguage.setSelection(targetPos)
                binding.spinnerTargetLanguage.setSelection(inputPos)
            } else {
                Toast.makeText(
                    this,
                    "Select both languages before swapping.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnReset.setOnClickListener {
            resetAll()
        }
    }

    private fun validateLanguages(): Boolean {
        if (selectedInputLang == null) {
            Toast.makeText(this, "Please select input language.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedTargetLang == null) {
            Toast.makeText(this, "Please select target language.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // ---------------------- RECORDING FLOW (DUMMY) ----------------------

    private fun toggleRecording() {
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        if (!validateLanguages()) return

        isRecording = true
        binding.tvStatus.text = getString(R.string.status_listening)

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

        binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.recording)

        binding.fabMicrophone.postDelayed({
            if (isRecording) stopRecording()
        }, 3000)
    }

    private fun stopRecording() {
        isRecording = false
        binding.tvStatus.text = getString(R.string.status_processing)

        binding.fabMicrophone.clearAnimation()
        binding.fabMicrophone.scaleX = 1f
        binding.fabMicrophone.scaleY = 1f
        binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.primary)

        startLoadingUI()

        binding.fabMicrophone.postDelayed({
            onTranslationReady(durationMs = 5000L)
        }, 2000)
    }

    // ---------------------- AUDIO UI HELPERS ----------------------

    private fun startLoadingUI() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvAudioStatus.text = "Translating and generating audio..."
        binding.tvAudioDuration.text = "00:00"
    }

    private fun onTranslationReady(durationMs: Long) {
        binding.progressLoading.visibility = View.GONE
        binding.tvAudioStatus.text = "Translation ready"

        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.tvAudioDuration.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun resetAll() {
        binding.spinnerInputLanguage.setSelection(0)
        binding.spinnerTargetLanguage.setSelection(0)
        selectedInputLang = null
        selectedTargetLang = null

        isRecording = false
        binding.fabMicrophone.clearAnimation()
        binding.fabMicrophone.scaleX = 1f
        binding.fabMicrophone.scaleY = 1f
        binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.primary)

        binding.tvStatus.text = getString(R.string.status_tap_mic)
        binding.tvAudioStatus.text = "Waiting for translation..."
        binding.tvAudioDuration.text = "00:00"
        binding.progressLoading.visibility = View.GONE

        Toast.makeText(this, "Reset complete.", Toast.LENGTH_SHORT).show()
    }

    private fun performSignOut() {

        // Clear stored session
        val prefs = getSharedPreferences("SpeechSyncPrefs", MODE_PRIVATE)
        prefs.edit()
            .clear()
            .apply()

        // Go back to LoginActivity & clear back stack
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Kill current activity
        finish()
    }

}
