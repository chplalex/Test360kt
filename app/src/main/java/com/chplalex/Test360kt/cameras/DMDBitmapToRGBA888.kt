package com.chplalex.Test360kt.cameras

import android.graphics.Bitmap
import android.graphics.Color.*
import java.nio.ByteBuffer

class DMDBitmapToRGBA888 {

    companion object {

        @JvmStatic
        fun imageToRGBA8888(bmp: Bitmap?): ByteArray? {
            if (bmp == null) return null
            val bytes = ByteArray(bmp.allocationByteCount)
            val bb = ByteBuffer.wrap(bytes)
            bb.asIntBuffer().put(getPixels(bmp))
            return bytes
        }

        private fun getPixels(bmp: Bitmap): IntArray? {
            val height = bmp.height
            val width = bmp.width
            val length = width * height
            val pixels = IntArray(length)
            bmp.getPixels(pixels, 0, width, 0, 0, width, height)
            for (i in pixels.indices) {
                var p = pixels[i]
                pixels[i] = (red(p) shl 24) or (green(p) shl 16) or (blue(p) shl 8) or (alpha(p))
            }
            return pixels
        }

    }
}