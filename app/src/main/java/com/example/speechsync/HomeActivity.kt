package com.example.speechsync

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.speechsync.databinding.ActivityHomeBinding
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.io.File

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var translationService: TranslationService
    private lateinit var audioPlayer: AudioPlayer

    private var recordedFile: File? = null
    private var selectedInputLang: String? = null
    private var selectedTargetLang: String? = null

    private val langCodeMap = mapOf(
        "English" to "en",
        "Hindi" to "hi",
        "Bengali" to "bn",
        "Odia" to "od"
    )

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //UI color match done
        window.statusBarColor = getColor(R.color.primary)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false


        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarHome)
        setSupportActionBar(toolbar)

        setSupportActionBar(binding.toolbarHome)

        audioRecorder = AudioRecorder(this)
        translationService = TranslationService()
        audioPlayer = AudioPlayer(this)

        setupButtons()
        checkPermissions()
        setupLanguageSpinners()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions required for recording", Toast.LENGTH_LONG).show()
            }
        }
    }

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

    private fun setupLanguageSpinners() {
        val languages = resources.getStringArray(R.array.languages_list).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)

        binding.spinnerInputLanguage.adapter = adapter
        binding.spinnerTargetLanguage.adapter = adapter

        binding.spinnerInputLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedInputLang = if (position == 0) null else langCodeMap[languages[position]]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerTargetLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTargetLang = if (position == 0) null else langCodeMap[languages[position]]

                if (selectedInputLang != null && selectedTargetLang != null && selectedInputLang == selectedTargetLang) {
                    Toast.makeText(this@HomeActivity, "Input and target cannot be same", Toast.LENGTH_SHORT).show()
                    binding.spinnerTargetLanguage.setSelection(0)
                    selectedTargetLang = null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        binding.fabMicrophone.setOnClickListener {
            toggleRecording()
        }

        binding.btnTranslate.setOnClickListener {
            performTranslation()
        }

        binding.btnPlayPause.setOnClickListener {
            togglePlayback()
        }

        binding.btnRetry.setOnClickListener {
            resetUI()
        }

        binding.btnSwapLanguages.setOnClickListener {
            swapLanguages()
        }

        binding.btnReset.setOnClickListener {
            resetAll()
        }
    }

    private fun toggleRecording() {
        if (!audioRecorder.isRecording()) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        if (!validateLanguages()) return

        try {
            recordedFile = audioRecorder.startRecording()
            binding.tvStatus.text = getString(R.string.status_listening)
            animateMicrophone(true)
            binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.recording)
        } catch (e: Exception) {
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            recordedFile = audioRecorder.stopRecording()
            binding.tvStatus.text = "Recording saved. Tap Translate."
            animateMicrophone(false)
            binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.primary)
        } catch (e: Exception) {
            Toast.makeText(this, "Stop recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performTranslation() {
        if (!validateLanguages()) return

        if (selectedInputLang == "bn" || selectedInputLang == "od" ||
            selectedTargetLang == "bn" || selectedTargetLang == "od") {
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
            return
        }

        val file = recordedFile
        if (file == null) {
            Toast.makeText(this, "Please record audio first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show initial processing state
        binding.tvStatus.text = getString(R.string.status_processing)
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvAudioStatus.text = "Uploading audio..."

        // Disable translate button to prevent multiple requests
        binding.btnTranslate.isEnabled = false

        lifecycleScope.launch {
            // Update status messages periodically
            binding.tvAudioStatus.text = "Processing audio on server..."

            translationService.translate(file, selectedInputLang!!, selectedTargetLang!!)
                .onSuccess { response ->
                    runOnUiThread {
                        binding.progressLoading.visibility = View.GONE
                        binding.tvStatus.text = "Translation complete"
                        binding.tvAudioStatus.text = "Translation: ${response.translatedText}"
                        binding.btnTranslate.isEnabled = true

                        // Play audio
                        audioPlayer.play(response.audioUrl) {
                            runOnUiThread {
                                binding.btnPlayPause.setIconResource(android.R.drawable.ic_media_play)
                            }
                        }

                        val duration = audioPlayer.getDuration()
                        if (duration > 0) {
                            val seconds = duration / 1000
                            binding.tvAudioDuration.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
                        }

                        binding.btnPlayPause.setIconResource(android.R.drawable.ic_media_pause)
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        binding.progressLoading.visibility = View.GONE
                        binding.tvStatus.text = "Translation failed"
                        binding.btnTranslate.isEnabled = true

                        val errorMessage = when {
                            error.message?.contains("timeout", ignoreCase = true) == true ->
                                "Request timed out. Please try again with shorter audio."
                            error.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                                "Network error. Check your internet connection."
                            else -> error.message ?: "Unknown error occurred"
                        }

                        binding.tvAudioStatus.text = "Error: $errorMessage"
                        Toast.makeText(
                            this@HomeActivity,
                            "Translation failed: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun togglePlayback() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause()
            binding.btnPlayPause.setIconResource(android.R.drawable.ic_media_play)
        } else {
            audioPlayer.resume()
            binding.btnPlayPause.setIconResource(android.R.drawable.ic_media_pause)
        }
    }

    //gpt generated animation
    private fun animateMicrophone(start: Boolean) {
        if (start) {
            // Microphone pulse animation
            val scaleX = ObjectAnimator.ofFloat(binding.fabMicrophone, "scaleX", 1f, 1.1f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.fabMicrophone, "scaleY", 1f, 1.1f, 1f)
            scaleX.duration = 1000
            scaleY.duration = 1000
            scaleX.repeatCount = ObjectAnimator.INFINITE
            scaleY.repeatCount = ObjectAnimator.INFINITE
            scaleX.interpolator = AccelerateDecelerateInterpolator()
            scaleY.interpolator = AccelerateDecelerateInterpolator()
            scaleX.start()
            scaleY.start()

            // Show and animate wave circles
            startWaveAnimations()
        } else {
            binding.fabMicrophone.clearAnimation()
            binding.fabMicrophone.scaleX = 1f
            binding.fabMicrophone.scaleY = 1f

            // Stop wave animations
            stopWaveAnimations()
        }
    }

    private fun startWaveAnimations() {
        // Make circles visible
        binding.waveCircle1.visibility = View.VISIBLE
        binding.waveCircle2.visibility = View.VISIBLE
        binding.waveCircle3.visibility = View.VISIBLE

        val animationDuration = 1500L // 1.5 seconds per wave
        val delayBetweenWaves = 500L // 0.5 seconds between each wave

        // Wave Circle 1 - First wave
        val scale1X = ObjectAnimator.ofFloat(binding.waveCircle1, "scaleX", 1f, 2.5f)
        val scale1Y = ObjectAnimator.ofFloat(binding.waveCircle1, "scaleY", 1f, 2.5f)
        val alpha1 = ObjectAnimator.ofFloat(binding.waveCircle1, "alpha", 0.7f, 0f)
        scale1X.duration = animationDuration
        scale1Y.duration = animationDuration
        alpha1.duration = animationDuration
        scale1X.repeatCount = ObjectAnimator.INFINITE
        scale1Y.repeatCount = ObjectAnimator.INFINITE
        alpha1.repeatCount = ObjectAnimator.INFINITE
        scale1X.interpolator = AccelerateDecelerateInterpolator()
        scale1Y.interpolator = AccelerateDecelerateInterpolator()
        alpha1.interpolator = AccelerateDecelerateInterpolator()

        // Wave Circle 2 - Second wave (delayed)
        val scale2X = ObjectAnimator.ofFloat(binding.waveCircle2, "scaleX", 1f, 2.5f)
        val scale2Y = ObjectAnimator.ofFloat(binding.waveCircle2, "scaleY", 1f, 2.5f)
        val alpha2 = ObjectAnimator.ofFloat(binding.waveCircle2, "alpha", 0.7f, 0f)
        scale2X.duration = animationDuration
        scale2Y.duration = animationDuration
        alpha2.duration = animationDuration
        scale2X.repeatCount = ObjectAnimator.INFINITE
        scale2Y.repeatCount = ObjectAnimator.INFINITE
        alpha2.repeatCount = ObjectAnimator.INFINITE
        scale2X.interpolator = AccelerateDecelerateInterpolator()
        scale2Y.interpolator = AccelerateDecelerateInterpolator()
        alpha2.interpolator = AccelerateDecelerateInterpolator()
        scale2X.startDelay = delayBetweenWaves
        scale2Y.startDelay = delayBetweenWaves
        alpha2.startDelay = delayBetweenWaves

        // Wave Circle 3 - Third wave (delayed more)
        val scale3X = ObjectAnimator.ofFloat(binding.waveCircle3, "scaleX", 1f, 2.5f)
        val scale3Y = ObjectAnimator.ofFloat(binding.waveCircle3, "scaleY", 1f, 2.5f)
        val alpha3 = ObjectAnimator.ofFloat(binding.waveCircle3, "alpha", 0.7f, 0f)
        scale3X.duration = animationDuration
        scale3Y.duration = animationDuration
        alpha3.duration = animationDuration
        scale3X.repeatCount = ObjectAnimator.INFINITE
        scale3Y.repeatCount = ObjectAnimator.INFINITE
        alpha3.repeatCount = ObjectAnimator.INFINITE
        scale3X.interpolator = AccelerateDecelerateInterpolator()
        scale3Y.interpolator = AccelerateDecelerateInterpolator()
        alpha3.interpolator = AccelerateDecelerateInterpolator()
        scale3X.startDelay = delayBetweenWaves * 2
        scale3Y.startDelay = delayBetweenWaves * 2
        alpha3.startDelay = delayBetweenWaves * 2

        // Start all animations
        scale1X.start()
        scale1Y.start()
        alpha1.start()
        scale2X.start()
        scale2Y.start()
        alpha2.start()
        scale3X.start()
        scale3Y.start()
        alpha3.start()
    }

    private fun stopWaveAnimations() {
        // Fade out and hide circles
        val fadeOut1 = ObjectAnimator.ofFloat(binding.waveCircle1, "alpha", binding.waveCircle1.alpha, 0f)
        val fadeOut2 = ObjectAnimator.ofFloat(binding.waveCircle2, "alpha", binding.waveCircle2.alpha, 0f)
        val fadeOut3 = ObjectAnimator.ofFloat(binding.waveCircle3, "alpha", binding.waveCircle3.alpha, 0f)

        fadeOut1.duration = 300
        fadeOut2.duration = 300
        fadeOut3.duration = 300

        fadeOut1.start()
        fadeOut2.start()
        fadeOut3.start()

        // Hide after fade out
        binding.waveCircle1.postDelayed({
            binding.waveCircle1.visibility = View.INVISIBLE
            binding.waveCircle2.visibility = View.INVISIBLE
            binding.waveCircle3.visibility = View.INVISIBLE
            binding.waveCircle1.clearAnimation()
            binding.waveCircle2.clearAnimation()
            binding.waveCircle3.clearAnimation()
        }, 300)
    }

    //language check
    private fun validateLanguages(): Boolean {
        if (selectedInputLang == null) {
            Toast.makeText(this, "Select input language", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedTargetLang == null) {
            Toast.makeText(this, "Select target language", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


    //swap
    private fun swapLanguages() {
        val inputPos = binding.spinnerInputLanguage.selectedItemPosition
        val targetPos = binding.spinnerTargetLanguage.selectedItemPosition

        if (inputPos != 0 && targetPos != 0) {
            binding.spinnerInputLanguage.setSelection(targetPos)
            binding.spinnerTargetLanguage.setSelection(inputPos)
        } else {
            Toast.makeText(this, "Select both languages first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetUI() {
        binding.tvStatus.text = getString(R.string.status_tap_mic)
        binding.tvAudioStatus.text = "Waiting for translation..."
        binding.tvAudioDuration.text = "00:00"
        binding.progressLoading.visibility = View.GONE
        binding.btnTranslate.isEnabled = true
        audioPlayer.stop()
        recordedFile = null
    }

    private fun resetAll() {
        resetUI()
        binding.spinnerInputLanguage.setSelection(0)
        binding.spinnerTargetLanguage.setSelection(0)
        selectedInputLang = null
        selectedTargetLang = null
        animateMicrophone(false)
        binding.fabMicrophone.backgroundTintList = getColorStateList(R.color.primary)
        Toast.makeText(this, "Reset complete", Toast.LENGTH_SHORT).show()
    }

    private fun performSignOut() {
        getSharedPreferences("SpeechSyncPrefs", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}