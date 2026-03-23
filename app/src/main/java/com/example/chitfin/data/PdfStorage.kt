package com.example.chitfin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}