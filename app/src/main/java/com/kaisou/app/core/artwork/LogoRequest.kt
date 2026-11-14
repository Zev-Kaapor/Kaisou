package com.kaisou.app.core.artwork

// Android framework
import android.content.Context
// Image loading
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size

// The request of a logo, the page and the preload build it the same way so the one that is downloaded first is the one that
// is found in the memory cache. Its pixels are read to turn down the black ones, which a hardware picture does not allow
fun logoRequest(context: Context, url: String, size: Size): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(size)
        .allowHardware(false)
        .build()
