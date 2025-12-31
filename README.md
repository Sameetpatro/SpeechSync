# 🎙️ Design and Implementation of an Android-Based Speech Translation Application for Indic Languages

## 📌 Overview

This project is an **Android-based speech-to-speech translation application** that converts spoken audio from one language into **spoken output in another language**, with primary focus on **Indic languages**.

The application uses:
- **Kotlin (Android)** for the frontend
- **Flask (Python)** as the backend server
- Deep learning models for **Speech-to-Text, Translation, and Text-to-Speech**

The Android app communicates with the Flask server through a REST API.

---

## 🧠 System Architecture


::contentReference[oaicite:0]{index=0}


**Flow:**
1. User records speech in the Android app  
2. Audio is sent to Flask backend  
3. Backend performs:
   - Speech Recognition
   - Language Translation
   - Speech Synthesis  
4. Translated speech is sent back and played on the device  

---

## 🛠️ Tech Stack

### Android (Frontend)
- Kotlin
- Android Studio
- Audio Recorder
- HTTP API communication

### Backend (Flask Server)
- Python
- Flask
- PyTorch
- Hugging Face Transformers
- Whisper (Speech-to-Text)
- NLLB / IndicTrans (Translation)
- gTTS / audio processing libraries
- NumPy, Librosa, SoundFile

---

## 📂 Backend Repository

Backend code is available here:  
🔗 https://github.com/Sameetpatro/ss_pipeline_B

---

## 🚀 Backend Setup (Step-by-Step)

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/Sameetpatro/ss_pipeline_B.git
cd ss_pipeline_B
