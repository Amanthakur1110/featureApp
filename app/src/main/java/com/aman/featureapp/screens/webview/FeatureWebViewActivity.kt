package com.aman.featureapp.screens.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aman.featureapp.bridge.CameraCaptureHandler
import com.aman.featureapp.bridge.JSBridgeRegistry
import com.aman.featureapp.constant.AppConstant
import com.aman.featureapp.session.UserSession
import com.google.gson.Gson
import java.io.File

class FeatureWebViewActivity : ComponentActivity(), CameraCaptureHandler {

    companion object {
        const val EXTRA_FEATURE_UID = "extra_feature_uid"
        const val EXTRA_FEATURE_NAME = "extra_feature_name"
        const val EXTRA_IS_PUBLIC = "extra_is_public"
    }

    private var featureUid: String = ""
    private var featureName: String = ""
    private var isPublic: Boolean = false

    private var webView: WebView? = null
    private var loadProgress by mutableFloatStateOf(0f)
    private var isPageLoading by mutableStateOf(true)
    private var hasLoadError by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")

    // Camera bridge capture state
    private var pendingCameraCallback: String? = null
    private var pendingCameraFile: File? = null

    // HTML5 File Chooser callback (<input type="file">)
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val cb = pendingCameraCallback
        val file = pendingCameraFile

        if (cb != null && file != null) {
            val result = if (success && file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                mapOf(
                    "success" to true,
                    "filename" to file.name,
                    "path" to file.absolutePath,
                    "base64" to "data:image/jpeg;base64,$base64"
                )
            } else {
                mapOf("success" to false, "error" to "Capture cancelled or failed")
            }

            val json = Gson().toJson(result).replace("'", "\\'")
            runOnUiThread {
                webView?.evaluateJavascript("if (window['$cb']) { window['$cb']($json); }", null)
            }
        }
        pendingCameraCallback = null
        pendingCameraFile = null
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        fileChooserCallback?.onReceiveValue(uris?.toTypedArray() ?: emptyArray())
        fileChooserCallback = null
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCameraInternal()
        } else {
            pendingCameraCallback?.let { cb ->
                val json = Gson().toJson(mapOf("success" to false, "error" to "Camera permission denied"))
                webView?.evaluateJavascript("if (window['$cb']) { window['$cb']($json); }", null)
            }
            pendingCameraCallback = null
            pendingCameraFile = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        featureUid = intent.getStringExtra(EXTRA_FEATURE_UID) ?: ""
        featureName = intent.getStringExtra(EXTRA_FEATURE_NAME) ?: "Feature App"
        isPublic = intent.getBooleanExtra(EXTRA_IS_PUBLIC, false)

        android.util.Log.d(
            "FeatureWebView",
            ">>> [ACTIVITY CREATED] uid=$featureUid, name=$featureName, isPublic=$isPublic"
        )

        setContent {
            FeatureWebViewScreen(
                title = featureName,
                progress = loadProgress,
                isLoading = isPageLoading,
                hasError = hasLoadError,
                errorMessage = errorMessage,
                onBack = { handleBackPressed() },
                onRefresh = {
                    hasLoadError = false
                    isPageLoading = true
                    android.util.Log.d("FeatureWebView", ">>> [REFRESH TRIGGERED]")
                    loadFeatureUrl()
                },
                onWebViewCreated = { wv ->
                    setupWebView(wv)
                }
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(wv: WebView) {
        this.webView = wv

        wv.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        WebView.setWebContentsDebuggingEnabled(true)

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
        }

        wv.isVerticalScrollBarEnabled = false
        wv.isHorizontalScrollBarEnabled = false
        wv.overScrollMode = View.OVER_SCROLL_NEVER

        // Register all native bridges
        JSBridgeRegistry.registerBridges(this, wv, featureUid, this)

        wv.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                val lvl = consoleMessage?.messageLevel()
                val msg = consoleMessage?.message()
                val src = consoleMessage?.sourceId()
                val line = consoleMessage?.lineNumber()
                when (lvl) {
                    android.webkit.ConsoleMessage.MessageLevel.ERROR -> {
                        android.util.Log.e("FeatureWebView", ">>> [JS ERROR] $msg (source: $src:$line)")
                    }
                    android.webkit.ConsoleMessage.MessageLevel.WARNING -> {
                        android.util.Log.w("FeatureWebView", ">>> [JS WARN] $msg (source: $src:$line)")
                    }
                    else -> {
                        android.util.Log.d("FeatureWebView", ">>> [JS LOG] $msg")
                    }
                }
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadProgress = newProgress / 100f
                isPageLoading = newProgress < 100
                if (newProgress == 100) {
                    android.util.Log.d("FeatureWebView", ">>> [PROGRESS 100%] WebView finished loading progress")
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                android.util.Log.d("FeatureWebView", ">>> [TITLE RECEIVED] title=$title")
                if (!title.isNullOrBlank() && !title.startsWith("http")) {
                    featureName = title
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                runOnUiThread {
                    request?.grant(request.resources)
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                filePickerLauncher.launch("*/*")
                return true
            }
        }

        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                android.util.Log.d("FeatureWebView", ">>> [PAGE STARTED] url=$url")
                if (url != "about:blank") {
                    hasLoadError = false
                    isPageLoading = true
                    // Inject early error and resource failure listeners
                    view?.evaluateJavascript("""
                        (function() {
                            if (window.__debug_listeners_installed) return;
                            window.__debug_listeners_installed = true;
                            window.addEventListener('error', function(event) {
                                if (event.target && (event.target.tagName === 'SCRIPT' || event.target.tagName === 'LINK')) {
                                    console.error('[RESOURCE LOAD ERROR] Failed to load ' + event.target.tagName + ': ' + (event.target.src || event.target.href));
                                } else {
                                    console.error('[WINDOW ONERROR] ' + (event.message || event.error) + ' at ' + (event.filename || '') + ':' + (event.lineno || 0) + ':' + (event.colno || 0) + (event.error && event.error.stack ? '\n' + event.error.stack : ''));
                                }
                            }, true);
                            window.addEventListener('unhandledrejection', function(event) {
                                var reason = event.reason;
                                console.error('[UNHANDLED REJECTION] ' + (reason && reason.stack ? reason.stack : reason));
                            });
                            console.log('[DEBUG-WV] Early error listeners attached successfully.');
                        })();
                    """.trimIndent(), null)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                android.util.Log.d("FeatureWebView", ">>> [PAGE FINISHED] url=$url")
                if (url != "about:blank") {
                    isPageLoading = false
                    // Comprehensive DOM, Bridge, and Mount diagnostics
                    view?.evaluateJavascript("""
                        (function() {
                            function inspect(stage) {
                                console.log('[DEBUG-WV ' + stage + '] URL: ' + window.location.href);
                                console.log('[DEBUG-WV ' + stage + '] Title: ' + document.title + ' | ReadyState: ' + document.readyState);
                                console.log('[DEBUG-WV ' + stage + '] Window: ' + window.innerWidth + 'x' + window.innerHeight);

                                // Check bridges
                                var bridges = ['AndroidStorage', 'AndroidTorch', 'AndroidMic', 'AndroidCamera', 'AndroidLocation', 'AndroidFile'];
                                var bridgeStatus = bridges.map(function(b) { return b + ':' + (typeof window[b]); }).join(', ');
                                console.log('[DEBUG-WV ' + stage + '] Bridges: ' + bridgeStatus);

                                // Check scripts
                                var scripts = Array.from(document.querySelectorAll('script')).map(function(s) {
                                    return (s.src ? s.src.split('/').pop() : 'inline') + (s.defer ? '[defer]' : '') + (s.async ? '[async]' : '');
                                }).join(', ');
                                console.log('[DEBUG-WV ' + stage + '] Scripts: ' + (scripts || 'NONE'));

                                // Check #root
                                var root = document.getElementById('root');
                                if (!root) {
                                    console.error('[DEBUG-WV ' + stage + '] CRITICAL: #root element NOT found in DOM!');
                                    console.log('[DEBUG-WV ' + stage + '] Body snippet: ' + document.body.innerHTML.substring(0, 400));
                                    return;
                                }

                                var children = root.children.length;
                                var textLen = (root.textContent || '').trim().length;
                                var rect = root.getBoundingClientRect();
                                var computed = window.getComputedStyle(root);

                                console.log('[DEBUG-WV ' + stage + '] #root children=' + children + ', textLen=' + textLen + ', size=' + Math.round(rect.width) + 'x' + Math.round(rect.height) + ', display=' + computed.display + ', visibility=' + computed.visibility);

                                if (children === 0 && textLen === 0) {
                                    console.warn('[DEBUG-WV ' + stage + '] #root is currently EMPTY! React component has not mounted yet.');
                                    console.log('[DEBUG-WV ' + stage + '] Body HTML: ' + document.body.innerHTML.substring(0, 500));
                                } else {
                                    var firstChildTag = root.firstElementChild ? root.firstElementChild.tagName.toLowerCase() : 'text';
                                    console.log('[DEBUG-WV ' + stage + '] SUCCESS: React mounted into #root! First child: <' + firstChildTag + '>');
                                }
                            }

                            inspect('0ms');
                            setTimeout(function() { inspect('500ms'); }, 500);
                            setTimeout(function() { inspect('1500ms'); }, 1500);
                        })();
                    """.trimIndent(), null)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                val isMain = request?.isForMainFrame == true
                val reqUrl = request?.url?.toString()
                val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description else "Error"
                val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode else -1

                android.util.Log.e(
                    "FeatureWebView",
                    ">>> [NETWORK ERROR] mainFrame=$isMain, code=$code, desc=$desc, url=$reqUrl"
                )

                if (isMain) {
                    hasLoadError = true
                    isPageLoading = false
                    errorMessage = "Connection failed ($code): $desc"
                    view?.stopLoading()
                    view?.loadUrl("about:blank")
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                val isMain = request?.isForMainFrame == true
                val reqUrl = request?.url?.toString()
                val statusCode = errorResponse?.statusCode ?: -1
                val reason = errorResponse?.reasonPhrase ?: ""

                android.util.Log.e(
                    "FeatureWebView",
                    ">>> [HTTP ERROR $statusCode] mainFrame=$isMain, reason=$reason, url=$reqUrl"
                )

                if (isMain && statusCode >= 400) {
                    hasLoadError = true
                    isPageLoading = false
                    errorMessage = "Server error ($statusCode): $reason"
                    view?.stopLoading()
                    view?.loadUrl("about:blank")
                }
            }
        }

        loadFeatureUrl()
    }

    private fun loadFeatureUrl() {
        val token = if (!isPublic) UserSession.getToken() else null
        val targetUrl = AppConstant.Network.getFeatureHtmlUrl(featureUid, token)
        android.util.Log.d(
            "FeatureWebView",
            ">>> [LOADING URL] targetUrl=$targetUrl (isPublic=$isPublic, tokenPresent=${token != null})"
        )
        webView?.loadUrl(targetUrl)
    }

    private fun handleBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPressed()
    }

    // ── CameraCaptureHandler implementation ─────────────────────────────────

    override fun launchCameraCapture(customFilename: String?, callbackJsFunction: String) {
        pendingCameraCallback = callbackJsFunction

        val dir = File(filesDir, "features/$featureUid").apply { if (!exists()) mkdirs() }
        val filename = if (!customFilename.isNullOrBlank()) {
            if (customFilename.endsWith(".jpg") || customFilename.endsWith(".png")) customFilename else "$customFilename.jpg"
        } else {
            "img_${System.currentTimeMillis()}.jpg"
        }
        val photoFile = File(dir, filename)
        pendingCameraFile = photoFile

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraInternal()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraInternal() {
        val file = pendingCameraFile ?: return
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )
        takePictureLauncher.launch(uri)
    }
}

@Composable
fun FeatureWebViewScreen(
    title: String,
    progress: Float,
    isLoading: Boolean,
    hasError: Boolean,
    errorMessage: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // ── Native Top Bar (Feels like a real native screen) ─────────
            Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A)
                            )
                        }

                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasError) Color(0xFFDC2626)
                                            else if (isLoading) Color(0xFFEAB308)
                                            else Color(0xFF16A34A)
                                        )
                                )
                                Text(
                                    text = if (hasError) "Offline" else if (isLoading) "Loading..." else "Native Feature",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(Color(0xFFE2E8F0))
                )
            }

            // ── Content Area ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Actual Native WebView
                AndroidView(
                    factory = { context ->
                        WebView(context).also { onWebViewCreated(it) }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Dedicated Native Loading UI (Never shows blank white web screen)
                if (isLoading && !hasError) {
                    NativeLoadingOverlay(title = title, progress = progress)
                }

                // Custom Premium Error / Offline UI (NO ugly Chromium error page)
                if (hasError) {
                    CustomErrorOverlay(
                        errorMessage = errorMessage,
                        onRetry = onRefresh,
                        onClose = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun NativeLoadingOverlay(
    title: String,
    progress: Float
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color(0xFF0F172A),
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Launching $title",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Preparing native components and bridges...",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF0F172A),
                trackColor = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
private fun CustomErrorOverlay(
    errorMessage: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Offline",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Unable to Load Feature",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (errorMessage.isNotBlank()) errorMessage
                else "Please check your internet connection or backend server status and try again.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
