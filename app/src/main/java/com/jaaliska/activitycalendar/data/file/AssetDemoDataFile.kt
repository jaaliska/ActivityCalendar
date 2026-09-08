package com.jaaliska.activitycalendar.data.file

import android.content.res.AssetManager
import com.jaaliska.activitycalendar.domain.DemoDataFile
import java.io.InputStream

class AssetDemoDataFile(private val assets: AssetManager) : DemoDataFile {

    override fun open(): InputStream = assets.open(FILE_NAME)

    private companion object {
        const val FILE_NAME = "demo-activities.csv"
    }
}
