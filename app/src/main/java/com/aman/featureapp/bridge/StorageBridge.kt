package com.aman.featureapp.bridge

import android.content.Context
import android.content.SharedPreferences
import android.webkit.JavascriptInterface
import com.google.gson.Gson

/**
 * Scoped key/value storage bridge exposed as `window.AndroidStorage`.
 * Each feature has its own SharedPreferences file keyed by featureUid.
 */
class StorageBridge(
    context: Context,
    private val featureUid: String
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "feature_storage_$featureUid",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    @JavascriptInterface
    fun setItem(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    @JavascriptInterface
    fun getItem(key: String): String? {
        return prefs.getString(key, null)
    }

    @JavascriptInterface
    fun removeItem(key: String) {
        prefs.edit().remove(key).apply()
    }

    @JavascriptInterface
    fun clear() {
        prefs.edit().clear().apply()
    }

    @JavascriptInterface
    fun getAllKeys(): String {
        val keys = prefs.all.keys
        return gson.toJson(keys)
    }
}
