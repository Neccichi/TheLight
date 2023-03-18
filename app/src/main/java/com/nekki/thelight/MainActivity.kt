package com.nekki.thelight

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MainActivity : AppCompatActivity() {
    private lateinit var mainh: TextView
    private lateinit var autoCompleteStreetText: AutoCompleteTextView

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private val streetsListKyiv = mutableListOf<String>()
    private val streetsListDnipro = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainh = findViewById(R.id.h1MainText)
        autoCompleteStreetText = findViewById(R.id.autoStreetText)

        val regionSpinner = findViewById<Spinner>(R.id.regionSpinner)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.regions_array, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        regionSpinner.adapter = adapter

        coroutineScope.launch {
            getWeb()
        }

        regionSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when (position) {
                    0 -> updateAutoCompleteTextView(streetsListKyiv)
                    1 -> updateAutoCompleteTextView(streetsListDnipro)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        })
    }

    private fun updateAutoCompleteTextView(streetsList: List<String>) {
        val streetAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            streetsList
        )
        autoCompleteStreetText.setAdapter(streetAdapter)
    }

    private suspend fun getWeb() {
        try {
            val doc: Document = withContext(Dispatchers.IO) {
                Jsoup.connect("https://www.dtek-kem.com.ua/ua/shutdowns")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .get()
            }
            //Вулиці Києва
            val totalPagesKyiv = 20 // Вкажіть кількість сторінок, які потрібно проітерувати
            for (page in 1..totalPagesKyiv) {
                val urlStreetsKyiv = "https://locator.ua/ua/list/kyiv/streets/n$page/"
                val docStreetsKyiv: Document = withContext(Dispatchers.IO) {
                    Jsoup.connect(urlStreetsKyiv).get()
                }
                val elementsStreetsKyiv = docStreetsKyiv.select("tbody tr")

                for (row in elementsStreetsKyiv) {
                    val streetColumnKyiv = row.select("td:nth-child(1)")
                    if (streetColumnKyiv.isNotEmpty()) {
                        val streetName = streetColumnKyiv.text()
                        this.streetsListKyiv.add(streetName)
                    }
                }
            }

            Log.d("Streets Kyiv List", streetsListKyiv.toString())

            //Вулиці Дніпра
            val totalPagesDnipro = 10 // Вкажіть кількість сторінок, які потрібно проітерувати
            for (page in 1..totalPagesDnipro) {
                val urlStreetsDnipro = "https://dp.locator.ua/ua/list/dnipro/streets/n$page/"
                val docStreetsDnipro: Document = withContext(Dispatchers.IO) {
                    Jsoup.connect(urlStreetsDnipro).get()
                }
                val elementsStreetsDnipro = docStreetsDnipro.select("tbody tr")

                for (row in elementsStreetsDnipro) {
                    val streetColumnDnipro = row.select("td:nth-child(1)")
                    if (streetColumnDnipro.isNotEmpty()) {
                        val streetName = streetColumnDnipro.text()
                        this.streetsListDnipro.add(streetName)
                    }
                }
            }

            Log.d("Streets Dnipro List", streetsListDnipro.toString())

            val headerJson = doc.select("script[type=application/json]").first()?.data()
            val headerElement = JSONObject(headerJson).getJSONArray("pageProps").getJSONObject(0).getString("title")
            withContext(Dispatchers.Main) {
                mainh.text = headerElement
            }


            Log.d("MainActivity", "Title: $headerElement")
        } catch (e: Exception) {
            when (e) {
                is CancellationException -> {
                    Log.e("MainActivity", "Відміна завантаження", e)
                }
                is HttpStatusException -> {
                    withContext(Dispatchers.Main) {
                        mainh.text = "Помилка при завантаженні контенту: ${e.message}"
                    }
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        mainh.text = "Помилка при завантаженні контенту"
                    }
                    Log.e("MainActivity", "Помилка при завантаженні контенту", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}