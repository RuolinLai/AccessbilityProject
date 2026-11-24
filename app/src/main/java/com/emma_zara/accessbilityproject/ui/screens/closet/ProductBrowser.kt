package com.emma_zara.accessbilityproject.ui.screens.closet

import com.emma_zara.accessbilityproject.ui.components.LocalBottomBarSpace
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.emma_zara.accessbilityproject.closet.ProductLink
import com.emma_zara.accessbilityproject.ui.components.AText
import org.json.JSONArray
import org.json.JSONObject

/**
 * What the user chose in the app's browser: the product photo's address (if the page has one),
 * the product name, and a picture of what was on the screen (used if the photo can't be downloaded).
 */
data class BrowserPick(
    val imageUrl: String?,
    val title: String?,
    val pageUrl: String,
    val screenshot: Bitmap?,
    val userAgent: String,
    val cookies: String?,
)

/** Finds the product photo in the open page: the share picture (og:image), or else the biggest picture on the screen. */
private const val FIND_PRODUCT_JS = """
(function() {
  function meta(k) {
    var e = document.querySelector('meta[property="' + k + '"], meta[name="' + k + '"]');
    return e && e.content ? e.content : null;
  }
  var img = meta('og:image') || meta('og:image:secure_url') || meta('twitter:image');
  if (!img) {
    var best = null, area = 0;
    document.querySelectorAll('img').forEach(function(i) {
      var r = i.getBoundingClientRect();
      var a = Math.max(0, Math.min(r.bottom, innerHeight) - Math.max(r.top, 0)) * r.width;
      if (a > area && i.currentSrc) { area = a; best = i.currentSrc; }
    });
    img = best;
  }
  return JSON.stringify({
    image: img ? new URL(img, location.href).href : null,
    title: meta('og:title') || document.title,
    url: location.href
  });
})()
"""

/**
 * A browser inside the app, for websites that only show their pages to real browsers (like lululemon).
 * The user sees the product page, then taps "Use this product".
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ProductBrowser(url: String, message: String?, onClose: () -> Unit, onPick: (BrowserPick) -> Unit) {
    val context = LocalContext.current
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true   // most shopping websites need it
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    canGoBack = view.canGoBack()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress
                }
            }
            loadUrl(url)
        }
    }
    DisposableEffect(webView) { onDispose { webView.destroy() } }

    // The back button goes back in the website first, then closes the browser
    BackHandler { if (webView.canGoBack()) webView.goBack() else onClose() }

    fun pick() {
        busy = true
        webView.evaluateJavascript(FIND_PRODUCT_JS) { result ->
            busy = false
            val info = try {
                // The result is the JSON text, written as a JavaScript string
                JSONObject(JSONArray("[$result]").getString(0))
            } catch (e: Exception) {
                null
            }
            val shot = try {
                Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888).also { webView.draw(Canvas(it)) }
            } catch (e: Exception) {
                null
            }
            val pageUrl = info?.optString("url")?.takeIf { it.isNotBlank() } ?: webView.url ?: url
            onPick(
                BrowserPick(
                    imageUrl = info?.optString("image")?.takeIf { it.isNotBlank() && it != "null" },
                    title = info?.optString("title")?.let { ProductLink.cleanTitle(ProductLink.decodeEntities(it)) },
                    pageUrl = pageUrl,
                    screenshot = shot,
                    userAgent = webView.settings.userAgentString,
                    cookies = CookieManager.getInstance().getCookie(pageUrl),
                ),
            )
        }
    }

    Column(Modifier.fillMaxSize().padding(bottom = LocalBottomBarSpace.current)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AText(
                (message?.let { "$it " } ?: "") + "When you see the product, tap \"Use this product\".",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = 56.dp)) { AText("Close") }
                if (canGoBack) {
                    OutlinedButton(onClick = { webView.goBack() }, modifier = Modifier.heightIn(min = 56.dp)) { AText("Back") }
                }
                Button(
                    onClick = ::pick,
                    enabled = !busy && progress >= 30,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) { AText("Use this product") }
            }
        }
        if (progress < 100) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}
