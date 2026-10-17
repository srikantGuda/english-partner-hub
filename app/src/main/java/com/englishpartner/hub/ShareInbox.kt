package com.englishpartner.hub

import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Content shared into the app (from WhatsApp, Files, Gallery...) waiting for the user to pick a day. */
data class PendingShare(
    val uris: List<Uri>,
    val text: String?,
    val subject: String?
) {
    val isEmpty: Boolean get() = uris.isEmpty() && text.isNullOrBlank()
}

class ShareInbox {
    private val _pending = MutableStateFlow<PendingShare?>(null)
    val pending: StateFlow<PendingShare?> = _pending.asStateFlow()

    fun accept(intent: Intent?): Boolean {
        val share = parse(intent) ?: return false
        if (share.isEmpty) return false
        _pending.value = share
        return true
    }

    fun clear() {
        _pending.value = null
    }

    companion object {
        fun parse(intent: Intent?): PendingShare? {
            if (intent == null) return null
            return when (intent.action) {
                Intent.ACTION_SEND -> {
                    val uri = intent.streamUri()
                    PendingShare(
                        uris = listOfNotNull(uri),
                        text = if (uri == null) intent.getStringExtra(Intent.EXTRA_TEXT) else null,
                        subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                    )
                }
                Intent.ACTION_SEND_MULTIPLE -> PendingShare(
                    uris = intent.streamUris(),
                    text = null,
                    subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                )
                else -> null
            }
        }

        @Suppress("DEPRECATION")
        private fun Intent.streamUri(): Uri? =
            if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        @Suppress("DEPRECATION")
        private fun Intent.streamUris(): List<Uri> {
            val list: List<Uri>? =
                if (Build.VERSION.SDK_INT >= 33) getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            return list.orEmpty()
        }
    }
}
