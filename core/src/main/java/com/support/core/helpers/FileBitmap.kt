package com.support.core.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.support.core.Inject
import java.io.File
import java.io.FileNotFoundException

@Inject(true)
class FileBitmap(private val context: Context) {

    fun isExists(uri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT > 23) {
            try {
                context.contentResolver.openInputStream(uri)
                true
            } catch (e: FileNotFoundException) {
                false
            }
        } else {
            FileUtils.getPath(context, uri)?.let { File(it).exists() } ?: false
        }
    }

    fun getBitmapFrom(uri: Uri): Bitmap {
        val bitmap: Bitmap =
            FileUtils.getPath(context, uri)?.let { BitmapFactory.decodeFile(it) } ?: try {
                val ims = context.contentResolver.openInputStream(uri)
                    ?: throw FileNotFoundException("File $uri not found")
                BitmapFactory.decodeStream(ims)
            } catch (e: FileNotFoundException) {
                throw e
            }

        val exif: ExifInterface? = if (Build.VERSION.SDK_INT > 23)
            context.contentResolver.openInputStream(uri)?.let { ExifInterface(it) }
        else uri.path?.let { ExifInterface(it) }

        val orientation: Int = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        ) ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            else -> bitmap
        }

    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        ).also { source.recycle() }
    }
}