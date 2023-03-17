package com.nekki.thelight

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import kotlinx.coroutines.*
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MainActivity : AppCompatActivity() {
    private lateinit var textViewTest: TextView
    private lateinit var mainh: TextView

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textViewTest = findViewById(R.id.MainTextTest)
        mainh = findViewById(R.id.h1)
        val regionSpinner = findViewById<Spinner>(R.id.regionSpinner)
        val adapter = ArrayAdapter.createFromResource(this,
            R.array.regions_array, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        regionSpinner.adapter = adapter

        coroutineScope.launch {
            getWeb()
        }
    }

    private suspend fun getWeb() {
        try {
            val doc: Document = withContext(Dispatchers.IO) {
                Jsoup.connect("https://kyiv.yasno.com.ua/schedule-turn-off-electricity").get()
            }
            //Вулиці


            val text = doc.title()

            if (::mainh.isInitialized) {
                mainh.text = text
            }

            Log.d("MainActivity", "Title: $text")
        } catch (e: Exception) {
            when (e) {
                is CancellationException -> {
                    Log.e("MainActivity", "Task was cancelled", e)
                }
                is HttpStatusException -> {
                    withContext(Dispatchers.Main) {
                        mainh.text = "Ошибка при загрузке контента: ${e.message}"
                    }
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        mainh.text = "Ошибка при загрузке контента"
                    }
                    Log.e("MainActivity", "Error while getting content", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
