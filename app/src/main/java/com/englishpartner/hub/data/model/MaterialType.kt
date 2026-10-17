package com.englishpartner.hub.data.model

/**
 * The kinds of course material the app understands.
 * Detection is pure Kotlin (no Android classes) so it can be unit tested on the JVM.
 */
enum class MaterialType(val label: String) {
    AUDIO("Audio"),
    VIDEO("Video"),
    PDF("PDF"),
    IMAGE("Image"),
    TEXT("Article"),
    OTHER("File");

    companion object {
        private val audioExt = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "amr", "3ga", "weba")
        private val videoExt = setOf("mp4", "mkv", "webm", "3gp", "mov", "m4v", "avi")
        private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
        private val textExt = setOf("txt", "md", "text", "csv", "html", "htm", "srt", "vtt")

        fun fromName(name: String?): MaterialType =
            entries.firstOrNull { it.name == name } ?: OTHER

        /** Decide the type from MIME type first, then fall back to the file extension. */
        fun detect(mimeType: String?, fileName: String?): MaterialType {
            val mime = mimeType?.lowercase()?.trim().orEmpty()
            when {
                mime.startsWith("audio/") || mime == "application/ogg" -> return AUDIO
                mime.startsWith("video/") -> return VIDEO
                mime == "application/pdf" -> return PDF
                mime.startsWith("image/") -> return IMAGE
                mime.startsWith("text/") -> return TEXT
            }
            return when (extensionOf(fileName)) {
                in audioExt -> AUDIO
                in videoExt -> VIDEO
                "pdf" -> PDF
                in imageExt -> IMAGE
                in textExt -> TEXT
                else -> OTHER
            }
        }

        fun extensionOf(fileName: String?): String {
            if (fileName.isNullOrBlank()) return ""
            val dot = fileName.lastIndexOf('.')
            if (dot < 0 || dot == fileName.length - 1) return ""
            return fileName.substring(dot + 1).lowercase()
        }
    }
}
