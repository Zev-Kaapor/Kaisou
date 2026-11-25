package com.kaisou.app.core.download

// Android framework
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri

// Saves a picture in the Kaisou folder of the pictures through the system downloader, which shows its own notice
object ImageDownload {
    // Name of the folder of the pictures
    private const val Folder = "Kaisou"

    // Starts the download of the picture with the name given for the file, true when the system took it
    fun save(context: Context, address: String, fileName: String): Boolean =
        try {
            val request = DownloadManager.Request(address.toUri())
                .setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "$Folder/$fileName")
            context.getSystemService(DownloadManager::class.java).enqueue(request)
            true
        } catch (_: Exception) {
            false
        }

    // The name of the file from the address of the picture, with the extension it has
    fun nameOf(address: String, prefix: String): String = "$prefix-${address.substringAfterLast('/').substringBefore('?')}"
}
