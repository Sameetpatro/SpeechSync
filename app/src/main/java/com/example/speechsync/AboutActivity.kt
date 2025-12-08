package com.example.speechsync

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Supervisor link
        val tvSupLink = findViewById<TextView>(R.id.tvSupervisorLinkedIn)
        val tvSupUrlHidden = findViewById<TextView>(R.id.tvSupervisorLinkedInUrl)

        tvSupLink.setOnClickListener {
            val url = tvSupUrlHidden.text.toString().trim()
            if (url.isNotEmpty()) {
                openUrl(url)
            }
        }

        // Developer link
        val tvDevLink = findViewById<TextView>(R.id.tvDevLinkedIn)
        val tvDevUrlHidden = findViewById<TextView>(R.id.tvDevLinkedInUrl)

        tvDevLink.setOnClickListener {
            val url = tvDevUrlHidden.text.toString().trim()
            if (url.isNotEmpty()) {
                openUrl(url)
            }
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        // Optional: make sure there is an app to handle it
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }
}
