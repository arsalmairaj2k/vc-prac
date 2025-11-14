package com.arsalmairaj.gemwebapi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button // Import Button
import android.widget.EditText // Import EditText
import android.widget.ProgressBar // Import ProgressBar
import android.widget.TextView // Import TextView
import androidx.core.content.ContextCompat // Import ContextCompat
import okhttp3.* // OkHttp library for networking
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Declare the view variables
    private lateinit var etInputText: EditText
    private lateinit var btnCheckSpam: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvResult: TextView

    private val client = OkHttpClient()

    // IMPORTANT:
    // If you are running this on an Android Emulator, 10.0.2.2 points to your computer's localhost.
    // If you are using a real Android device, you MUST replace 10.0.2.2
    // with your computer's IP address on the local network (e.g., 192.168.1.10).
    // Your phone and computer must be on the same WiFi network.
    private val apiUrl = "http://192.168.18.80:5002/abc"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view directly from the layout file
        setContentView(R.layout.activity_main)

        // Initialize the views using findViewById
        etInputText = findViewById(R.id.et_input_text)
        btnCheckSpam = findViewById(R.id.btn_check_spam)
        pbLoading = findViewById(R.id.pb_loading)
        tvResult = findViewById(R.id.tv_result)

        // Set a click listener for the button
        btnCheckSpam.setOnClickListener {
            val inputText = etInputText.text.toString()
            if (inputText.isNotEmpty()) {
                checkSpam(inputText)
            } else {
                tvResult.text = "Please enter some text to check."
            }
        }
    }

    private fun checkSpam(text: String) {
        // Show loading spinner and hide the result text
        pbLoading.visibility = View.VISIBLE
        tvResult.visibility = View.GONE
        btnCheckSpam.isEnabled = false // Disable button during request

        // Create the form body with the 'text' key, as required by your API
        val formBody = FormBody.Builder()
            .add("text", text)
            .build()

        // Build the POST request
        val request = Request.Builder()
            .url(apiUrl)
            .post(formBody)
            .build()

        // Make the network call asynchronously
        client.newCall(request).enqueue(object : Callback {

            // Handle network request failure
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // Update the UI on the main thread
                runOnUiThread {
                    pbLoading.visibility = View.GONE
                    tvResult.visibility = View.VISIBLE
                    btnCheckSpam.isEnabled = true
                    tvResult.text = "Network Error: ${e.message}"
                    // Use ContextCompat.getColor with the correct context
                    tvResult.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                }
            }

            // Handle network request success
            override fun onResponse(call: Call, response: Response) {
                // --- THIS IS THE FIX ---
                // Read the response body on the background thread first.
                // This is a blocking operation and MUST NOT be in runOnUiThread.
                val responseBody = response.body?.string()

                // Now, update the UI on the main thread
                runOnUiThread {
                    pbLoading.visibility = View.GONE
                    tvResult.visibility = View.VISIBLE
                    btnCheckSpam.isEnabled = true

                    try {
                        if (response.isSuccessful) {
                            // The responseBody was already fetched on the background thread

                            // Parse the response from your API (e.g., "['ham']")
                            val result = parseApiResponse(responseBody)

                            // Display the final result
                            tvResult.text = "Result: $result"

                            // Optionally change color based on result
                            if (result.equals("Spam", ignoreCase = true)) {
                                // Use ContextCompat.getColor with the correct context
                                tvResult.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                            } else {
                                // Use ContextCompat.getColor with the correct context
                                tvResult.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                            }

                        } else {
                            // Show error code if request was not successful
                            tvResult.text = "Error: ${response.code}"
                            // Use ContextCompat.getColor with the correct context
                            tvResult.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Updated error message to show exception type
                        tvResult.text = "Error: ${e.javaClass.simpleName} - ${e.message}"
                        // Use ContextCompat.getColor with the correct context
                        tvResult.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                    }
                }
            }
        })
    }

    /**
     * Parses the raw string response from the API.
     * Your API returns "['ham']" or "['spam']".
     * This function cleans it up to "Ham" or "Spam".
     */
    private fun parseApiResponse(responseBody: String?): String {
        if (responseBody == null) return "No response"

        // Cleans the string: "['ham']" -> "ham"
        val cleanedResult = responseBody
            .replace("['", "")
            .replace("']", "")
            .replace("\"", "") // Just in case
            .replace("[", "")
            .replace("]", "")
            .trim()

        // Capitalizes the first letter: "ham" -> "Ham"
        return cleanedResult.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}