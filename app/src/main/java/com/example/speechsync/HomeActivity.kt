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
import androidx.lifecycle.lifecycleScope
import com.example.speechsync.databinding.ActivityHomeBinding
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

        setSupportActionBar(binding.toolbarHome)

        audioRecorder = AudioRecorder(this)
        translationService = TranslationService()
        audioPlayer = AudioPlayer(this)

        checkPermissions()
        setupLanguageSpinners()
        setupButtons()
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

        // Check if Bengali or Odia is selected
        if (selectedInputLang == "bn" || selectedInputLang == "od" ||
            selectedTargetLang == "bn" || selectedTargetLang == "od") {
            Toast.makeText(this, "Will be available soon", Toast.LENGTH_SHORT).show()
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

    private fun animateMicrophone(start: Boolean) {
        if (start) {
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
        } else {
            binding.fabMicrophone.clearAnimation()
            binding.fabMicrophone.scaleX = 1f
            binding.fabMicrophone.scaleY = 1f
        }
    }

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