package com.jaaliska.activitycalendar.ui.file

import android.net.Uri
import java.io.InputStream

/** Gives access to a file the user picked in the system file picker. */
interface FileSource {

    /**
     * Opens the file for reading.
     *
     * @param uri what the picker returned
     * @return the file's contents; the caller closes the stream
     * @throws java.io.IOException if the file cannot be opened
     */
    fun open(uri: Uri): InputStream

    /**
     * Reads the name to show the user.
     *
     * @param uri what the picker returned
     * @return the file name, or an empty string if the provider does not report one
     */
    fun displayName(uri: Uri): String
}
