package com.example.meddocsapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for recording audio notes for patients.
 * Provides controls to record, stop, play, and save audio.
 */
class AudioRecorderActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AudioRecorder"
        private const val PERMISSION_REQUEST_CODE = 200
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recordButton: ImageButton
    private lateinit var stopButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var saveButton: MaterialButton
    private lateinit var discardButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var chronometer: Chronometer
    private lateinit var patientNameText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var playbackTimeText: TextView

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var isPlaying = false
    private var patientId: Long = 0
    private var patientName: String = ""
    private var hasUnsavedRecording = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    seekBar.progress = player.currentPosition
                    playbackTimeText.text = formatTime(player.currentPosition)
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)

        patientId = intent.getLongExtra("PATIENT_ID", 0)
        patientName = intent.getStringExtra("PATIENT_NAME") ?: "Unknown"

        initViews()
        setupToolbar()
        setupButtons()
        setupBackPressHandler()
        checkPermissions()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recordButton = findViewById(R.id.record_button)
        stopButton = findViewById(R.id.stop_button)
        playButton = findViewById(R.id.play_button)
        saveButton = findViewById(R.id.save_button)
        discardButton = findViewById(R.id.discard_button)
        statusText = findViewById(R.id.status_text)
        chronometer = findViewById(R.id.chronometer)
        patientNameText = findViewById(R.id.patient_name_text)
        seekBar = findViewById(R.id.seek_bar)
        playbackTimeText = findViewById(R.id.playback_time_text)

        patientNameText.text = "Recording for: $patientName"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    playbackTimeText.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateUIState(RecordingState.IDLE)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Record Audio"
        toolbar.setNavigationOnClickListener { handleBackPress() }
    }

    private fun setupButtons() {
        recordButton.setOnClickListener { startRecording() }
        stopButton.setOnClickListener {
            if (isRecording) stopRecording() else stopPlayback()
        }
        playButton.setOnClickListener { togglePlayback() }
        saveButton.setOnClickListener { saveAndFinish() }
        discardButton.setOnClickListener { confirmDiscard() }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        if (hasUnsavedRecording) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Recording")
                .setMessage("You have an unsaved recording. What would you like to do?")
                .setPositiveButton("Save") { _, _ -> saveAndFinish() }
                .setNegativeButton("Discard") { _, _ -> discardAndFinish() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            finish()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio recording permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startRecording() {
        // Stop any playback first
        stopPlayback()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AUDIO_${timeStamp}.m4a"
        audioFilePath = File(filesDir, fileName).absolutePath

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }

            isRecording = true
            hasUnsavedRecording = true
            chronometer.base = SystemClock.elapsedRealtime()
            chronometer.start()
            updateUIState(RecordingState.RECORDING)
            AppLogger.d(TAG, "Recording started: $audioFilePath")

        } catch (e: IOException) {
            AppLogger.e(TAG, "Recording failed", e)
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            chronometer.stop()
            updateUIState(RecordingState.STOPPED)
            AppLogger.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping recording", e)
        }
    }

    private fun togglePlayback() {
        if (isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        audioFilePath?.let { path ->
            try {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setDataSource(path)
                    mediaPlayer?.prepare()
                    mediaPlayer?.let { player ->
                        seekBar.max = player.duration
                        playbackTimeText.text = "0:00 / ${formatTime(player.duration)}"
                    }
                    mediaPlayer?.setOnCompletionListener {
                        onPlaybackComplete()
                    }
                }

                mediaPlayer?.start()
                isPlaying = true
                updateUIState(RecordingState.PLAYING)
                handler.post(updateSeekBarRunnable)
                AppLogger.d(TAG, "Playback started")

            } catch (e: Exception) {
                AppLogger.e(TAG, "Playback failed", e)
                Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPlaybackComplete() {
        isPlaying = false
        updateUIState(RecordingState.STOPPED)
        handler.removeCallbacks(updateSeekBarRunnable)
        seekBar.progress = 0
        mediaPlayer?.let { player ->
            playbackTimeText.text = "0:00 / ${formatTime(player.duration)}"
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        handler.removeCallbacks(updateSeekBarRunnable)
        updateUIState(RecordingState.STOPPED)
    }

    private fun stopPlayback() {
        handler.removeCallbacks(updateSeekBarRunnable)
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        isPlaying = false
        seekBar.progress = 0
    }

    private fun formatTime(millis: Int): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }

    private fun saveAndFinish() {
        stopPlayback()
        audioFilePath?.let { path ->
            val resultIntent = Intent().apply {
                putExtra("AUDIO_FILE_PATH", path)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            hasUnsavedRecording = false
            finish()
        }
    }

    private fun confirmDiscard() {
        AlertDialog.Builder(this)
            .setTitle("Discard Recording")
            .setMessage("Are you sure you want to discard this recording?")
            .setPositiveButton("Discard") { _, _ -> discardAndFinish() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun discardAndFinish() {
        stopPlayback()
        audioFilePath?.let { path ->
            File(path).delete()
        }
        hasUnsavedRecording = false
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun updateUIState(state: RecordingState) {
        when (state) {
            RecordingState.IDLE -> {
                recordButton.isEnabled = true
                stopButton.isEnabled = false
                playButton.isEnabled = false
                saveButton.isEnabled = false
                discardButton.isEnabled = false
                statusText.text = "Tap record to start"
                seekBar.visibility = android.view.View.GONE
                playbackTimeText.visibility = android.view.View.GONE
                chronometer.visibility = android.view.View.VISIBLE
            }
            RecordingState.RECORDING -> {
                recordButton.isEnabled = false
                stopButton.isEnabled = true
                playButton.isEnabled = false
                saveButton.isEnabled = false
                discardButton.isEnabled = false
                statusText.text = "Recording..."
                seekBar.visibility = android.view.View.GONE
                playbackTimeText.visibility = android.view.View.GONE
                chronometer.visibility = android.view.View.VISIBLE
            }
            RecordingState.STOPPED -> {
                recordButton.isEnabled = true
                stopButton.isEnabled = false
                playButton.isEnabled = true
                saveButton.isEnabled = true
                discardButton.isEnabled = true
                statusText.text = "Tap play to listen"
                seekBar.visibility = android.view.View.VISIBLE
                playbackTimeText.visibility = android.view.View.VISIBLE
                chronometer.visibility = android.view.View.GONE
                playButton.setImageResource(R.drawable.ic_play)
            }
            RecordingState.PLAYING -> {
                recordButton.isEnabled = false
                stopButton.isEnabled = true
                playButton.isEnabled = true
                saveButton.isEnabled = false
                discardButton.isEnabled = false
                statusText.text = "Playing..."
                playButton.setImageResource(R.drawable.ic_pause)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
        if (isPlaying) {
            pausePlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBarRunnable)
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    enum class RecordingState {
        IDLE, RECORDING, STOPPED, PLAYING
    }
}

