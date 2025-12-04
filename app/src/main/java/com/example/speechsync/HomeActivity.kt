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
    private var selectedInputLang: String? = null
    private var selectedTargetLang: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLanguageSpinners()
        setupButtons()
    }

    // ---------------------- LANGUAGE SPINNERS ----------------------

    private fun setupLanguageSpinners() {
        // defined in strings.xml as languages_list
        val languages = resources.getStringArray(R.array.languages_list).toList()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )

        binding.spinnerInputLanguage.adapter = adapter
        binding.spinnerTargetLanguage.adapter = adapter

        // Input defaults to "Select language"
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
        setSupportActionBar(binding.toolbarHome)

        binding.toolbarHome.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_about -> {
                    Toast.makeText(this, "About VaaniBridge (placeholder)", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_sign_out -> {
                    // TODO: clear login prefs and go back to LoginActivity
                    Toast.makeText(this, "Sign out clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
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

                    // If both selected & same → toast + clear target
                    if (selectedInputLang != null &&
                        selectedTargetLang != null &&
                        selectedInputLang == selectedTargetLang
                    ) {
                        Toast.makeText(
                            this@HomeActivity,
                            "Input and target language cannot be the same. Please change target language.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Reset target spinner
                        binding.spinnerTargetLanguage.setSelection(0)
                        selectedTargetLang = null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ---------------------- BUTTON SETUP ----------------------

    private fun setupButtons() {

        // Mic button: start/stop recording
        binding.fabMicrophone.setOnClickListener {
            toggleRecording()
        }

        // "Do you want to continue?" – placeholder for now
        binding.btnContinueQuestion.setOnClickListener {
            Toast.makeText(
                this,
                "Continue logic will be handled with pause detection later.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Translate button: trigger backend later, for now simulate loading
        binding.btnTranslate.setOnClickListener {
            if (!validateLanguages()) return@setOnClickListener
            startLoadingUI()

            // Simulate backend delay → 2s then translation ready
            binding.btnTranslate.postDelayed({
                onTranslationReady(durationMs = 5000L)  // fake 5s audio
            }, 2000)
        }

        // Play/Pause: for now just toast
        binding.btnPlayPause.setOnClickListener {
            Toast.makeText(this, "Play/Pause clicked (dummy)", Toast.LENGTH_SHORT).show()
        }

        // Retry: let user record again, keep language selection
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

            if (inputPos != 0 && targetPos != 0) { // ignore "Select language"
                binding.spinnerInputLanguage.setSelection(targetPos)
                binding.spinnerTargetLanguage.setSelection(inputPos)
            } else {
                Toast.makeText(this, "Select both languages before swapping.", Toast.LENGTH_SHORT).show()
            }
        }

        // Reset: clear everything
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

        // pulsing animation on mic
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

        // simulate 3 sec recording
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

        // here you’d normally send audio to backend; we just show loading for now
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
        // reset languages
        binding.spinnerInputLanguage.setSelection(0)
        binding.spinnerTargetLanguage.setSelection(0)
        selectedInputLang = null
        selectedTargetLang = null

        // reset recording + audio state
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
}
