package com.aman.featureapp.bridge

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson

/**
 * Location bridge exposed as `window.AndroidLocation`.
 */
class LocationBridge(
    private val activity: Activity,
    private val webView: WebView
) {
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(activity)
    }
    private val gson = Gson()

    @JavascriptInterface
    fun getLocation() {
        getLocation("onLocationReceived")
    }

    @SuppressLint("MissingPermission")
    @JavascriptInterface
    fun getLocation(callbackJsFunction: String) {
        val hasFine = PermissionHelper.hasPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarse = PermissionHelper.hasPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (!hasFine && !hasCoarse) {
            val errorPayload = gson.toJson(mapOf("error" to "Location permission not granted"))
            sendJsResult(callbackJsFunction, errorPayload)
            return
        }

        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener(activity) { loc ->
                if (loc != null) {
                    val result = mapOf(
                        "latitude" to loc.latitude,
                        "longitude" to loc.longitude,
                        "accuracy" to loc.accuracy,
                        "altitude" to loc.altitude,
                        "time" to loc.time
                    )
                    sendJsResult(callbackJsFunction, gson.toJson(result))
                } else {
                    val errorPayload = gson.toJson(mapOf("error" to "Location unavailable"))
                    sendJsResult(callbackJsFunction, errorPayload)
                }
            }
            .addOnFailureListener(activity) { e ->
                val errorPayload = gson.toJson(mapOf("error" to (e.message ?: "Failed to get location")))
                sendJsResult(callbackJsFunction, errorPayload)
            }
    }

    private fun sendJsResult(callbackName: String, jsonString: String) {
        activity.runOnUiThread {
            val safeJson = jsonString.replace("'", "\\'")
            webView.evaluateJavascript("if (window['$callbackName']) { window['$callbackName']($safeJson); }", null)
        }
    }
}
