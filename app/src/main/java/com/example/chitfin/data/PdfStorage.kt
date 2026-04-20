package com.example.chitfin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

val Context.pdfDataStore: DataStore<Preferences> by preferencesDataStore(name = "pdf_list")

object PdfKeys {
    val PDF_FILES = stringSetPreferencesKey("pdf_files")
}


class PdfStorage(private val context: Context) {

    val pdfFilesFlow: Flow<Set<String>> = context.pdfDataStore.data.map { prefs ->
        prefs[PdfKeys.PDF_FILES] ?: emptySet()
    }

    suspend fun addPdfFile(fileName: String) {
        context.pdfDataStore.edit { prefs ->
            val current = prefs[PdfKeys.PDF_FILES]?.toMutableSet() ?: mutableSetOf()
            current.add(fileName)
            prefs[PdfKeys.PDF_FILES] = current
        }
    }

    suspend fun deletePdfFile(fileName: String) {
        context.pdfDataStore.edit { prefs ->
            val current = prefs[PdfKeys.PDF_FILES]?.toMutableSet() ?: mutableSetOf()
            current.remove(fileName)
            prefs[PdfKeys.PDF_FILES] = current
        }
    }

    suspend fun renamePdfFile(oldName: String, newName: String): Boolean {
        return try {
            val pdfDir = File(context.filesDir, "pdfs")
            val oldFile = File(pdfDir, oldName)
            val newFile = File(pdfDir, newName)

            if (oldFile.exists()) {
                oldFile.renameTo(newFile)

                // Обновляем список в DataStore
                context.pdfDataStore.edit { prefs ->
                    val current = prefs[PdfKeys.PDF_FILES]?.toMutableSet() ?: mutableSetOf()
                    current.remove(oldName)
                    current.add(newName)
                    prefs[PdfKeys.PDF_FILES] = current
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}