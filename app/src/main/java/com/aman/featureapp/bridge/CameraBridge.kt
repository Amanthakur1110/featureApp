package com.aman.featureapp.bridge

import android.app.Activity
import android.content.pm.PackageManager
import android.webkit.JavascriptInterface
import android.webkit.WebView

interface CameraCaptureHandler {
    fun launchCameraCapture(customFilename: String?, callbackJsFunction: String)
}

/**
 * Camera bridge exposed as `window.AndroidCamera`.
 */
class CameraBridge(
    private val activity: Activity,
    private val webView: WebView,
    private val captureHandler: CameraCaptureHandler
) {
    @JavascriptInterface
    fun capturePhoto() {
        capturePhoto("onPhotoCaptured")
    }

    @JavascriptInterface
    fun capturePhoto(callbackJsFunction: String) {
        activity.runOnUiThread {
            captureHandler.launchCameraCapture(null, callbackJsFunction)
        }
    }

    @JavascriptInterface
    fun captureNamedPhoto(filename: String, callbackJsFunction: String) {
        activity.runOnUiThread {
            captureHandler.launchCameraCapture(filename, callbackJsFunction)
        }
    }

    @JavascriptInterface
    fun isCameraAvailable(): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
}
