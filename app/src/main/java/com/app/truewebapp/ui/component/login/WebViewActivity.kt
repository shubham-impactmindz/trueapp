package com.app.truewebapp.ui.component.login

// Import required Android and third-party libraries
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.truewebapp.databinding.ActivityWebViewBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// WebViewActivity class inherits from AppCompatActivity to display web content
class WebViewActivity : AppCompatActivity() {

    // View Binding instance for accessing layout views safely
    private lateinit var binding: ActivityWebViewBinding

    // API endpoint URL to fetch terms and privacy page content
    private val url = "https://truewebapp.com/api/page"

    // onCreate() lifecycle method called when the Activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using View Binding
        binding = ActivityWebViewBinding.inflate(layoutInflater)

        // Set the root view of the layout as the content view
        setContentView(binding.root)

        // Handle system UI insets (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Get the system bars dimensions
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to avoid overlap with system bars
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        // Get the "title" and "url" passed from Intent extras
        val title = intent.getStringExtra("title") ?: ""
        val directUrl = intent.getStringExtra("url")

        // Set the header text to show the page title
        binding.tvHeader.text = title

        // Start shimmer animation (loading effect)
        binding.shimmerLayout.startShimmer()

        // Make shimmer visible and WebView hidden initially
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.webView.visibility = View.GONE

        // If direct URL is provided, load it directly; otherwise fetch from API
        if (!directUrl.isNullOrEmpty()) {
            loadUrlIntoWebView(directUrl)
        } else {
            // Decide which type of page to load: "terms" or "privacy"
            val type = if (title.equals("Terms and Conditions", ignoreCase = true)) "terms" else "privacy"
        // Fetch HTML content from API and load it into WebView
        fetchHtmlContentAndLoad(type)
        }

        // Handle back button click from custom toolbar
        binding.backLayout.setOnClickListener {
            // Trigger default back navigation
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // Function to fetch HTML content from API and load into WebView
    private fun fetchHtmlContentAndLoad(type: String) {
        // Run network call in a background thread (to avoid blocking UI thread)
        Thread {
            try {
                // Initialize OkHttp client
                val client = OkHttpClient()

                // Build GET request for the provided URL
                val request = Request.Builder().url(url).build()

                // Execute the request synchronously
                val response = client.newCall(request).execute()

                // Extract response body as string
                val responseBody = response.body?.string()

                // Check if response is not null
                if (responseBody != null) {
                    // Parse response JSON
                    val json = JSONObject(responseBody)

                    // Check "success" field in response
                    val success = json.optBoolean("success", false)

                    if (success) {
                        // Extract HTML content based on type (terms or privacy)
                        val htmlContent = when (type) {
                            "terms" -> json.getJSONObject("terms").getString("page_content")
                            "privacy" -> json.getJSONObject("privacy").getString("page_content")
                            else -> ""
                        }

                        // Run on UI thread to load content into WebView
                        runOnUiThread {
                            loadHtmlIntoWebView(htmlContent)
                        }
                    } else {
                        // If API response indicates failure
                        runOnUiThread {
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.webView.visibility = View.VISIBLE
                            showToast("Failed to fetch page data")
                        }
                    }
                } else {
                    // If response body is null (empty response)
                    runOnUiThread {
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        binding.webView.visibility = View.VISIBLE
                        showToast("Empty response from server")
                    }
                }
            } catch (e: Exception) {
                // Catch and print exceptions (e.g., network error, parsing error)
                e.printStackTrace()
                runOnUiThread {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.webView.visibility = View.VISIBLE
                    showToast("Failed to load content")
                }
            }
        }.start() // Start the background thread
    }

    // Function to load a direct URL into WebView
    private fun loadUrlIntoWebView(url: String) {
        // Enable JavaScript support in WebView
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true

        // Set WebViewClient to handle page load events
        binding.webView.webViewClient = object : WebViewClient() {
            // Callback triggered when page finishes loading
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Stop shimmer animation once content is loaded
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE
            }
        }

        // Load the URL
        binding.webView.loadUrl(url)
    }

    // Function to load HTML string into WebView
    private fun loadHtmlIntoWebView(html: String) {
        // Enable JavaScript support in WebView
        binding.webView.settings.javaScriptEnabled = true

        // Load the HTML content into WebView
        binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        // Set WebViewClient to handle page load events
        binding.webView.webViewClient = object : WebViewClient() {
            // Callback triggered when page finishes loading
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Stop shimmer animation once content is loaded
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE
            }
        }
    }

    // Helper function to show a Toast message to the user
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this@WebViewActivity, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
