package com.kaisou.app.core.artwork

// Android framework
import android.content.Context
// Image loading
import coil3.request.ImageRequest
import coil3.size.Size

// The request of a poster or a backdrop, the page and the preload build it the same way so the one that is downloaded first is
// the one that is found in the memory cache
fun artRequest(context: Context, url: String, size: Size): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(size)
        .build()
