package com.aman.featureapp.bridge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.google.gson.Gson
import java.io.File

/**
 * Sandboxed file operations bridge exposed as `window.AndroidFile`.
 * Scoped to `<filesDir>/features/<featureUid>/`.
 */
class FileBridge(
    private val context: Context,
    private val featureUid: String
) {
    private val rootDir: File = File(context.filesDir, "features/$featureUid").apply {
        if (!exists()) mkdirs()
    }
    private val gson = Gson()

    private fun resolveSafeFile(relativePath: String): File? {
        val file = File(rootDir, relativePath).canonicalFile
        // Ensure path traversal cannot escape the feature folder
        if (!file.path.startsWith(rootDir.canonicalPath)) {
            return null
        }
        return file
    }

    @JavascriptInterface
    fun createFolder(folderName: String): Boolean {
        val target = resolveSafeFile(folderName) ?: return false
        return if (!target.exists()) target.mkdirs() else target.isDirectory
    }

    @JavascriptInterface
    fun writeTextFile(filename: String, text: String): Boolean {
        return try {
            val file = resolveSafeFile(filename) ?: return false
            file.parentFile?.mkdirs()
            file.writeText(text, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JavascriptInterface
    fun writeFile(filename: String, base64Content: String): Boolean {
        return try {
            val file = resolveSafeFile(filename) ?: return false
            file.parentFile?.mkdirs()
            val cleanBase64 = if (base64Content.contains(",")) {
                base64Content.substringAfter(",")
            } else {
                base64Content
            }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            file.writeBytes(bytes)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JavascriptInterface
    fun readTextFile(filename: String): String? {
        return try {
            val file = resolveSafeFile(filename) ?: return null
            if (file.exists() && file.isFile) file.readText(Charsets.UTF_8) else null
        } catch (e: Exception) {
            null
        }
    }

    @JavascriptInterface
    fun readFile(filename: String): String? {
        return try {
            val file = resolveSafeFile(filename) ?: return null
            if (file.exists() && file.isFile) {
                Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    @JavascriptInterface
    fun getAppDir(): String {
        return rootDir.absolutePath
    }

    @JavascriptInterface
    fun listFiles(): String {
        return listFiles(null)
    }

    @JavascriptInterface
    fun listFiles(subFolder: String?): String {
        val targetDir = if (subFolder.isNullOrBlank()) rootDir else (resolveSafeFile(subFolder) ?: rootDir)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return gson.toJson(emptyList<String>())
        }
        val names = targetDir.listFiles()?.map { it.name } ?: emptyList()
        return gson.toJson(names)
    }

    @JavascriptInterface
    fun deleteFile(filename: String): Boolean {
        val file = resolveSafeFile(filename) ?: return false
        return if (file.exists()) file.deleteRecursively() else false
    }

    @JavascriptInterface
    fun openFile(filename: String): Boolean {
        return try {
            val file = resolveSafeFile(filename) ?: return false
            if (!file.exists()) return false

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val extension = MimeTypeMap.getFileExtensionFromUrl(file.name)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
