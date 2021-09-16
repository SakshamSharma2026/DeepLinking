package com.codewithshadow.deeplinking

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.codewithshadow.deeplinking.databinding.ActivityMainBinding
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val defaultUrl: String = "https://www.google.co.in/"
    private val DOMAIN_URI_PREFIX: String = "https://deeplink00.page.link"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Set progress bar max value to 100
        binding.progressBar.max = 100


        // Add a action listener on edittext
        binding.searchUrl.setOnEditorActionListener(editorActionListener)


        // Open default url
        openWebsite(binding.searchUrl.text.toString())


        // Get Dynamic url from deeplink
        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                if (pendingDynamicLinkData != null) {
                    val getLink: Uri? = pendingDynamicLinkData.link
                    binding.searchUrl.setText(getLink.toString())
                    openWebsite(getLink.toString())
                } else {
                    binding.searchUrl.setText(defaultUrl)
                    openWebsite(defaultUrl)
                }
            }
            .addOnFailureListener(this) {}
    }

    private var editorActionListener: TextView.OnEditorActionListener? =
        TextView.OnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    openWebsite(binding.searchUrl.text.toString())
                }
            }
            false
        }

    @SuppressLint("SetJavaScriptEnabled")
    private fun openWebsite(urlLink: String) {
        // Set progress bar set value to 0
        binding.progressBar.progress = 0
        binding.webView.loadUrl(urlLink)
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                view?.loadUrl(url)
                binding.searchUrl.setText(request?.url.toString())
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                // Show progress bar when website is loading
                showProgressBar()
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Hide progress bar when website loaded
                hideProgressBar()
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                hideProgressBar()
                super.onReceivedError(view, request, error)
            }
        }
        val webViewClient: WebChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                // Progress bar progress is changing
                binding.progressBar.progress = newProgress
            }
        }
        binding.webView.webChromeClient = webViewClient
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.defaultTextEncodingName = "utf-8"
    }

    private fun createDeepLink(getUrl: String) {
        // Generating url
        val generateLink = Firebase.dynamicLinks.dynamicLink {
            link = Uri.parse(getUrl)
            domainUriPrefix = DOMAIN_URI_PREFIX
            androidParameters {
                DynamicLink.AndroidParameters.Builder()
                    .setMinimumVersion(1)
                    .build()
            }

            // Build short dynamic url for ease of sharing
            buildShortDynamicLink().addOnSuccessListener {
                // After successfully building short share calling shareUrl fun
                shareUrl(it.shortLink.toString())
            }.addOnFailureListener {
                it.printStackTrace()
            }
        }
    }

    private fun shareUrl(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Link")
        startActivity(Intent.createChooser(shareIntent, "Share..."))
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    fun showPopup(v: View) {
        val popup = PopupMenu(this, v)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.share_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.share_btn -> {
                    if (binding.searchUrl.text.toString() == defaultUrl) {
                        // If search url is not new than open default url
                        createDeepLink(defaultUrl)
                    } else {
                        // If search url is new than open new url
                        createDeepLink(binding.searchUrl.text.toString())
                    }
                }
            }
            true
        }
        popup.show()
    }

    override fun onBackPressed() {
        when {
            binding.webView.canGoBack() -> binding.webView.goBack()
            else -> super.onBackPressed()
        }
    }
}