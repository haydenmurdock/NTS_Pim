package com.example.nts_pim.utilities.international_phone_number

import okhttp3.*
import java.io.IOException

object CountryPhoneNumber {
    private const val address = "https://restcountries.eu/rest/v2/name/"
    var countryList = ""
    fun getCountryWithName(name: String):String {
        val client = OkHttpClient().newBuilder()
            .build()
        val url = address + name
        println("URL Country Code: $url")
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }
                    countryList = response.body.toString()
                }
            }
        })
        return countryList
    }
}
