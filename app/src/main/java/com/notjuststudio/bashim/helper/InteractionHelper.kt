package com.notjuststudio.bashim.helper

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.notjuststudio.bashim.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.content.FileProvider




class InteractionHelper(private val context: Context) {

    private val clipboard: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun toClipBoard(text: String) {
        val clip = ClipData.newPlainText("", text)
        clipboard.primaryClip = clip
    }

    fun share(activity: Activity, text: String) {
        val shareIntent = Intent().apply{
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        activity.startActivity(Intent.createChooser(shareIntent, context.resources.getText(R.string.info_text_share)))
    }

    fun share(activity: Activity, bitmap: Bitmap) {
        // save bitmap to cache directory
        try {

            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // don't forget to make the directory
            val stream = FileOutputStream("$cachePath/image.png") // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }

        val imagePath = File(context.cacheDir, "images")
        val newFile = File(imagePath, "image.png")
        val contentUri = FileProvider.getUriForFile(context, "com.notjuststudio.bashim.fileprovider", newFile)

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
            }
            activity.startActivity(Intent.createChooser(shareIntent, context.resources.getText(R.string.info_comics_share)))
        }
    }

}