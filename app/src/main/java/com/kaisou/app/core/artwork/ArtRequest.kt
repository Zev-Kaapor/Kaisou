package com.kaisou.app.core.artwork

// Android framework
import android.content.Context
// Image loading
import coil3.request.ImageRequest
import coil3.size.Size

// The request of a poster or a backdrop, built the same way by the page and the preload so the cache finds it
fun artRequest(context: Context, url: String, size: Size): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(size)
        .build()
