package com.nekki.thelight

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import android.webkit.WebView
import android.webkit.WebViewClient

class Result : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        val resultOfChoice = intent.getSerializableExtra("resultOfChoice") as? Pair<String, String>
        Log.d("ResultActivity", "Street: ${resultOfChoice?.first}, House Number: ${resultOfChoice?.second}")


        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val webView: WebView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        val url = "https://www.dtek-kem.com.ua/ua/shutdowns"
        webView.loadUrl(url)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                val resultOfChoice = intent.getSerializableExtra("resultOfChoice") as? Pair<String, String>
                Log.d("ResultOfChoice", "Street: ${resultOfChoice?.first}, House Number: ${resultOfChoice?.second}")

                resultOfChoice?.let {
                    view.evaluateJavascript("javascript:document.querySelector('#edit-field-street-value').value='${it.first}';", null)
                    view.evaluateJavascript("javascript:document.querySelector('#edit-field-house-value').value='${it.second}';", null)
                    view.evaluateJavascript("javascript:document.querySelector('#edit-submit-shutdowns').click();", null)
                }
            }
        }
    }
}