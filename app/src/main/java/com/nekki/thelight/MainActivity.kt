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
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {
    private lateinit var autoCompleteStreetText: AutoCompleteTextView
    private lateinit var autoCompleteHouseNumber: AutoCompleteTextView
    private lateinit var currentStreetTest: Pair<String, String>

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private val streetsListKyiv = mutableListOf<String>()
    private val streetsListDnipro = mutableListOf<String>()
    private val streetsUrlsKyiv = mutableListOf<String>()
    private val streetsUrlsDnipro = mutableListOf<String>()
    private var currentCity = "kyiv"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        autoCompleteStreetText = findViewById(R.id.autoStreetText)
        autoCompleteHouseNumber = findViewById(R.id.autoNumber)

        val regionSpinner = findViewById<Spinner>(R.id.regionSpinner)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.regions_array, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        regionSpinner.adapter = adapter

        coroutineScope.launch {
            getWeb()
            getWebKyiv()
            getWebDnipro()
        }

        regionSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        updateAutoCompleteTextView(streetsListKyiv)
                        currentCity = "kyiv"
                    }
                    1 -> {
                        updateAutoCompleteTextView(streetsListDnipro)
                        currentCity = "dnipro"
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        })
        autoCompleteStreetText.setOnItemClickListener { _, _, _, _ ->
            try {
                val selectedStreet = autoCompleteStreetText.text.toString()
                val position = if (currentCity == "kyiv") streetsListKyiv.indexOf(selectedStreet) else streetsListDnipro.indexOf(selectedStreet)
                val selectedUrl = if (currentCity == "kyiv") streetsUrlsKyiv[position] else streetsUrlsDnipro[position]
                val decodedUrl = URLDecoder.decode(selectedUrl, "UTF-8")
                currentStreetTest = Pair(selectedStreet, decodedUrl)
                Log.d("Selected Street", "Name: $selectedStreet, URL: $selectedUrl")
                coroutineScope.launch {
                    getHouseNumbers(decodedUrl)
                }
            } catch (e: Exception) {
                Log.e("Error", "Failed to process street selection: ${e.message}")
            }
        }

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


        } catch (e: Exception) {
        }
    }
    private suspend fun getHouseNumbers(url: String) {
        try {
            val houseNumbersDoc: Document = withContext(Dispatchers.IO) {
                Jsoup.connect(url).get()
            }

            val houseNumbersElements = houseNumbersDoc.select("a[href*=https://locator.ua/ua/b/]")
            val houseNumbersList = mutableListOf<String>()

            for (element in houseNumbersElements) {
                val houseNumber = element.text()
                houseNumbersList.add(houseNumber)
            }

            Log.d("House Numbers List", houseNumbersList.toString())
            updateAutoCompleteHouseNumber(houseNumbersList)

        } catch (e: HttpStatusException) {
            Log.e("Error", "Failed to load house numbers: ${e.message}")
        }
    }

    private fun updateAutoCompleteHouseNumber(houseNumbersList: List<String>) {
        val houseNumberAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            houseNumbersList
        )
        autoCompleteHouseNumber.setAdapter(houseNumberAdapter)
    }
    private suspend fun getWebKyiv() {
        //Вулиці Києва
        val urlStreetsKyiv = "https://locator.ua/ua/list/kyiv/streets/n1/"
        val docStreetsKyiv: Document = withContext(Dispatchers.IO) {
            Jsoup.connect(urlStreetsKyiv).get()
        }
        val elementsStreetsKyiv = docStreetsKyiv.select("tbody tr")

        for (row in elementsStreetsKyiv) {
            val streetColumnKyiv = row.select("td:nth-child(1)")
            val streetLink = row.select("td:nth-child(1) a").attr("href")
            if (streetColumnKyiv.isNotEmpty()) {
                val streetName = streetColumnKyiv.text()
                this.streetsListKyiv.add(streetName)
                this.streetsUrlsKyiv.add(streetLink)
            }
        }


        Log.d("Streets Kyiv List", streetsListKyiv.toString())
        Log.d("Streets Kyiv URLs", streetsUrlsKyiv.toString())


    }
    private suspend fun getWebDnipro() {
        //Вулиці Дніпра
        val urlStreetsDnipro = "https://dp.locator.ua/ua/list/dnipro/streets/n8/"
        val docStreetsDnipro: Document = withContext(Dispatchers.IO) {
            Jsoup.connect(urlStreetsDnipro).get()
        }
        val elementsStreetsDnipro = docStreetsDnipro.select("tbody tr")

        for (row in elementsStreetsDnipro) {
            val streetColumnDnipro = row.select("td:nth-child(1)")
            val streetLink = row.select("td:nth-child(1) a").attr("href")
            if (streetColumnDnipro.isNotEmpty()) {
                val streetName = streetColumnDnipro.text()
                this.streetsListDnipro.add(streetName)
                this.streetsUrlsDnipro.add(streetLink)
            }
        }

        Log.d("Streets Dnipro List", streetsListDnipro.toString())
        Log.d("Streets Dnipro URLs", streetsUrlsDnipro.toString())


    }


    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}