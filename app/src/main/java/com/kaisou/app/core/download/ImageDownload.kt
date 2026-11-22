package com.kaisou.app.core.download

// Android framework
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri

// Saves a picture of the internet in the folder Kaisou of the pictures of the phone, where the gallery shows it, through the downloader of the
// system, which shows its own notice while it goes and when it ends and keeps going if the app is closed
object ImageDownload {
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
