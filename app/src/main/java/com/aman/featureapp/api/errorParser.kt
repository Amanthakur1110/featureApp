package com.aman.featureapp.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException

/**
 * Parses the actual server-sent error message from an HttpException response body.
 *
 * The backend always returns:
 *   { "success": false, "message": "...", "data": null }
 *
 * If parsing fails, falls back to the provided [fallback] string.
 */
fun parseServerError(e: HttpException, fallback: String): String {
    return try {
        val errorBody = e.response()?.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            val json = Gson().fromJson(errorBody, JsonObject::class.java)
            json.get("message")?.asString?.takeIf { it.isNotBlank() } ?: fallback
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
