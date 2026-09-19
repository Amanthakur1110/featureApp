package com.aman.featureapp.bridge

import android.app.Activity
import android.webkit.WebView

object JSBridgeRegistry {
    fun registerBridges(
        activity: Activity,
        webView: WebView,
        featureUid: String,
        cameraHandler: CameraCaptureHandler
    ) {
        val storageBridge = StorageBridge(activity, featureUid)
        val fileBridge = FileBridge(activity, featureUid)
        val torchBridge = TorchBridge(activity)
        val micBridge = MicBridge(activity, featureUid)
        val locationBridge = LocationBridge(activity, webView)
        val cameraBridge = CameraBridge(activity, webView, cameraHandler)

        webView.addJavascriptInterface(storageBridge, JsBridgeConstants.STORAGE)
        webView.addJavascriptInterface(fileBridge, JsBridgeConstants.FILE)
        webView.addJavascriptInterface(torchBridge, JsBridgeConstants.TORCH)
        webView.addJavascriptInterface(micBridge, JsBridgeConstants.MIC)
        webView.addJavascriptInterface(locationBridge, JsBridgeConstants.LOCATION)
        webView.addJavascriptInterface(cameraBridge, JsBridgeConstants.CAMERA)
    }
}
