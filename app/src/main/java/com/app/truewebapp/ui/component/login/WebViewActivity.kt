package com.app.truewebapp.ui.component.login

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.databinding.ActivityWebViewBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding
    private val url = "https://truewebapp.com/api/page"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("title") ?: ""
        val type = if (title.equals("Terms and Conditions", ignoreCase = true)) "terms" else "privacy"

        binding.tvHeader.text = title
        binding.progressBarLayout.visibility = View.VISIBLE

        fetchHtmlContentAndLoad(type)

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun fetchHtmlContentAndLoad(type: String) {
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val htmlContent = when (type) {
                            "terms" -> json.getJSONObject("terms").getString("page_content")
                            "privacy" -> json.getJSONObject("privacy").getString("page_content")
                            else -> ""
                        }

                        runOnUiThread {
                            loadHtmlIntoWebView(htmlContent)
                        }
                    } else {
                        runOnUiThread {
                            binding.progressBarLayout.visibility = View.GONE
                            showToast("Failed to fetch page data")
                        }
                    }
                } else {
                    runOnUiThread {
                        binding.progressBarLayout.visibility = View.GONE
                        showToast("Empty response from server")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.progressBarLayout.visibility = View.GONE
                    showToast("Failed to load content")
                }
            }
        }.start()
    }

    private fun loadHtmlIntoWebView(html: String) {
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBarLayout.visibility = View.GONE
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this@WebViewActivity, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}


