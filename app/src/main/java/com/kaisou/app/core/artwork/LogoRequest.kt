package com.kaisou.app.core.artwork

// Android framework
import android.content.Context
// Image loading
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size

// The request of a logo, built the same way by the page and the preload, reading its pixels to turn down black ones
fun logoRequest(context: Context, url: String, size: Size): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(size)
        .allowHardware(false)
        .build()
