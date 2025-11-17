package io.github.lumkit.pline

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

internal lateinit var application: Application

class ApplicationProvider: ContentProvider() {

    override fun onCreate(): Boolean {
        application = context as Application
        return true
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String?>?
    ): Int = throw UnsupportedOperationException("Not yet implemented")

    override fun getType(uri: Uri): String = throw UnsupportedOperationException("Not yet implemented")

    override fun insert(uri: Uri, values: ContentValues?): Uri = throw UnsupportedOperationException("Not yet implemented")

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?
    ): Cursor = throw UnsupportedOperationException("Not yet implemented")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String?>?
    ): Int = throw UnsupportedOperationException("Not yet implemented")
}
