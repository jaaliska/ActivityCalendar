package com.jaaliska.activitycalendar.data.file

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.jaaliska.activitycalendar.ui.file.FileSource
import java.io.FileNotFoundException
import java.io.InputStream

class ContentFileSource(private val contentResolver: ContentResolver) : FileSource {

    override fun open(uri: Uri): InputStream =
        contentResolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())

    override fun displayName(uri: Uri): String {
        val columns = arrayOf(OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) return cursor.getString(column).orEmpty()
        }
        return uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    }
}
