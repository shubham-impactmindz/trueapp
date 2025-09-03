package com.app.truewebapp.ui.component

// Required imports for Android functionality
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.ActivitySplashBinding
import com.app.truewebapp.ui.base.BaseActivity
import com.app.truewebapp.ui.component.login.LoginActivity
import com.app.truewebapp.ui.component.main.MainActivity
import com.bumptech.glide.Glide

// Suppressing the warning for using a custom splash screen (since Android 12 introduced a default Splash API)
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() { // SplashActivity inherits from BaseActivity
    // View binding for accessing views in activity_splash.xml
    private lateinit var binding: ActivitySplashBinding

    // Lifecycle method called when activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen using AndroidX SplashScreen API
        val splashScreen = installSplashScreen()

        // Call the parent class's onCreate method
        super.onCreate(savedInstanceState)

        // Load and display GIF animation using Glide into the ImageView "logoImage"
        Glide.with(this)
            .asGif() // Tell Glide to handle the resource as GIF
            .load(R.drawable.launchscreen) // Load the drawable resource for splash animation
            .into(binding.logoImage) // Set it into the ImageView defined in layout

        // Post a delayed task on logoImage after 3 seconds (3000 ms)
        binding.logoImage.postDelayed({
            // Navigate to main screen after splash delay
            navigateToMainScreen()
        }, 3000) // Delay for 3 seconds
    }

    // Function to navigate user to the correct screen after splash
    private fun navigateToMainScreen() {
        // Access shared preferences using app-wide preference name
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)

        // Use Handler to execute navigation logic on main UI thread after small delay
        Handler(Looper.getMainLooper()).postDelayed({

            // Check if preferences contain a saved userId
            if (preferences.contains("userId")) {
                // If userId exists but is empty/null → go to LoginActivity
                if (preferences.getString("userId", "null").isNullOrEmpty()) {
                    val intent = Intent(this, LoginActivity::class.java) // Create intent for Login screen
                    startActivity(intent) // Start LoginActivity
                    finish() // Close SplashActivity
                } else {
                    // If valid userId exists → go to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent) // Start MainActivity
                    finish() // Close SplashActivity
                }
            } else {
                // If no userId exists in preferences → redirect to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

        }, 10) // Very short delay (10ms) for smooth navigation
    }

    // Abstract method from BaseActivity → used for observing ViewModel if needed
    override fun observeViewModel() {
        // No ViewModel observation in SplashActivity
    }

    // Initialize view binding for this activity
    override fun initViewBinding() {
        // Inflate layout via binding
        binding = ActivitySplashBinding.inflate(layoutInflater)

        // Get the root view of the binding
        val view = binding.root

        // Set root view as content view
        setContentView(view)
    }
}