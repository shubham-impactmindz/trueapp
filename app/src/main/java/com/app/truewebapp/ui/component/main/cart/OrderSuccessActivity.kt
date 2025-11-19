package com.app.truewebapp.ui.component.main.cart

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.truewebapp.R
import com.app.truewebapp.databinding.ActivityOrderSuccessBinding
import com.app.truewebapp.ui.component.main.MainActivity
import java.io.IOException

class OrderSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderSuccessBinding
    private var mediaPlayer: MediaPlayer? = null
    private var hasPlayedSound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Handle system bars insets (status + nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        // Initialize sound for sound effects
        initializeSound()

        // Play sound immediately when payment confirmation screen appears (only once)
        // Use post to ensure MediaPlayer is ready
        binding.root.post {
            playSoundEffect()
        }

        // Setup animation listener
        setupAnimationListener()
        
        // Make the view focusable to enable sound effects
        binding.lottieCheckmark.isFocusable = true
        binding.lottieCheckmark.isFocusableInTouchMode = true

        // Start Lottie animation
        binding.lottieCheckmark.playAnimation()

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        // Apply animations
        binding.textOrderPlaced.startAnimation(fadeIn)
        binding.textOrderMessage.startAnimation(fadeIn)
        binding.btnContinue.startAnimation(slideUp)

        // Handle button click
        binding.btnContinue.setOnClickListener {
            binding.btnContinue.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Initialize sound for playing sound effects from assets folder
     */
    private fun initializeSound() {
        try {
            // Load sound file from assets/sound/sound.mp3
            val assetFileDescriptor = assets.openFd("sound/sound.mp3")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                prepare()
            }
            assetFileDescriptor.close()
            Log.d("OrderSuccess", "Sound initialized successfully")
        } catch (e: IOException) {
            Log.e("OrderSuccess", "Error loading sound from assets: ${e.message}", e)
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e("OrderSuccess", "Error initializing MediaPlayer: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * Setup animation listener (sound is already played in onCreate)
     */
    private fun setupAnimationListener() {
        // Animation listener for any future animation-related logic
        binding.lottieCheckmark.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                // Sound is already played in onCreate
            }

            override fun onAnimationEnd(animation: Animator) {
                // Animation ended
            }

            override fun onAnimationCancel(animation: Animator) {
                // Handle cancellation if needed
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Animation repeat
            }
        })
    }

    /**
     * Play sound effect from assets folder and haptic feedback
     * Plays only once when payment confirmation screen appears
     */
    private fun playSoundEffect() {
        if (hasPlayedSound) {
            return // Already played, don't play again
        }
        
        try {
            // Check if device is not in silent mode
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val canPlaySound = audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
            
            // Play sound from assets folder (only once)
                mediaPlayer?.let { player ->
                if (canPlaySound && !player.isPlaying) {
                    // Reset to beginning and start playing
                        player.seekTo(0)
                        player.start()
                    hasPlayedSound = true
                    Log.d("OrderSuccess", "Sound started playing")
                } else if (!canPlaySound) {
                    Log.d("OrderSuccess", "Device is in silent mode, skipping sound")
                }
            } ?: run {
                Log.e("OrderSuccess", "MediaPlayer is null, cannot play sound")
            }
            
            // Use haptic feedback
            binding.lottieCheckmark.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (e: Exception) {
            Log.e("OrderSuccess", "Error playing sound effect: ${e.message}", e)
            e.printStackTrace()
            // Fallback: Just use haptic feedback
            binding.lottieCheckmark.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer resources
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
