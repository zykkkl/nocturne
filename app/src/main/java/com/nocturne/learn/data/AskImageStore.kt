package com.nocturne.learn.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 把相册/相机图片压到适合多模态模型的大小，存进 cache。
 * 只保留压缩后的副本，原始图片不会被读取第二次。
 */
object AskImageStore {

    private const val MAX_DIMENSION = 1400
    private const val JPEG_QUALITY = 84
    private const val MAX_AGE_MS = 3L * 24 * 60 * 60 * 1000

    fun directory(context: Context): File =
        File(context.cacheDir, "ask-images").apply { mkdirs() }

    fun newCaptureFile(context: Context): File {
        cleanup(context)
        return File(directory(context), "capture-${UUID.randomUUID()}.jpg")
    }

    fun prepare(context: Context, uri: Uri): String? = runCatching {
        cleanup(context)
        val bitmap = decodeSampled(context, uri) ?: return null
        val rotated = rotateByExif(context, uri, bitmap)
        val scaled = scaleDown(rotated, MAX_DIMENSION)
        if (scaled !== rotated) rotated.recycle()
        val target = File(directory(context), "ask-${UUID.randomUUID()}.jpg")
        FileOutputStream(target).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        scaled.recycle()
        target.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun decodeSampled(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun rotateByExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).rotationDegrees
            } ?: 0
        }.getOrDefault(0)
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun cleanup(context: Context) {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        directory(context).listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
    }
}
