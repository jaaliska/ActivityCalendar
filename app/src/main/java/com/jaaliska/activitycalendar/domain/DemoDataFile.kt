package com.jaaliska.activitycalendar.domain

import java.io.InputStream

/** The sample export shipped with the app. */
fun interface DemoDataFile {

    /** Opens the export for reading; the caller closes the stream. */
    fun open(): InputStream
}
