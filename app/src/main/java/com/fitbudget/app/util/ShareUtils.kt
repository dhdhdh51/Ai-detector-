package com.fitbudget.app.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a locally generated file to the system share sheet. Data only ever leaves the device when
 * the user explicitly picks a target app here.
 */
object ShareUtils {

    private const val TAG = "ShareUtils"

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (error: Exception) {
            Log.e(TAG, "Unable to share ${file.name}", error)
            Toast.makeText(context, "No app available to share this file.", Toast.LENGTH_LONG).show()
        }
    }

    fun shareText(context: Context, text: String, chooserTitle: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (error: Exception) {
            Log.e(TAG, "Unable to share text", error)
            Toast.makeText(context, "No app available to share this.", Toast.LENGTH_LONG).show()
        }
    }
}
