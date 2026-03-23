package com.example.chitfin.data  // или .utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun copyPdfToInternalStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    val pdfDir = File(context.filesDir, "pdfs")
    if (!pdfDir.exists()) pdfDir.mkdirs()

    val fileName = getFileNameFromUri(context, uri) ?: "file_${System.currentTimeMillis()}.pdf"
    val targetFile = File(pdfDir, fileName)

    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        fileName  // возвращаем имя
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex("_display_name")
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}