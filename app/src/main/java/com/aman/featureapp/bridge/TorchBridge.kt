package com.aman.featureapp.bridge

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.webkit.JavascriptInterface

/**
 * Torch bridge exposed as `window.AndroidTorch`.
 */
class TorchBridge(private val context: Context) {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private var isTorchEnabled = false

    private fun getCameraWithFlash(): String? {
        val manager = cameraManager ?: return null
        return try {
            manager.cameraIdList.firstOrNull { id ->
                val chars = manager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    @JavascriptInterface
    fun isAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) &&
                getCameraWithFlash() != null
    }

    @JavascriptInterface
    fun setTorch(enable: Boolean): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return false
        }
        val cameraId = getCameraWithFlash() ?: return false
        return try {
            cameraManager?.setTorchMode(cameraId, enable)
            isTorchEnabled = enable
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JavascriptInterface
    fun toggleTorch(): Boolean {
        return setTorch(!isTorchEnabled)
    }

    @JavascriptInterface
    fun isTorchOn(): Boolean {
        return isTorchEnabled
    }
}
